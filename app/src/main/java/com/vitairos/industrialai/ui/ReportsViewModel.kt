package com.vitairos.industrialai.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vitairos.industrialai.data.MockData
import com.vitairos.industrialai.data.SettingsStore
import com.vitairos.industrialai.data.local.AppDatabase
import com.vitairos.industrialai.data.local.ReportRepository
import com.vitairos.industrialai.model.Attachment
import com.vitairos.industrialai.model.AttachmentKind
import com.vitairos.industrialai.model.DemoUser
import com.vitairos.industrialai.model.ExtractedField
import com.vitairos.industrialai.model.Report
import com.vitairos.industrialai.model.ReportStatus
import com.vitairos.industrialai.model.TimelineEvent
import com.vitairos.industrialai.util.ConnectivityObserver
import com.vitairos.industrialai.util.ImageUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Reporte en edición/creación (antes de guardarse). */
data class Draft(
    val editingId: String? = null,
    val type: String = "",
    val process: String = "",
    val area: String = "",
    val shift: String = "",
    val notes: String = "",
    val attachments: List<Attachment> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val extracted: List<ExtractedField>? = null,
) {
    val metadataComplete get() = type.isNotBlank() && process.isNotBlank() && area.isNotBlank() && shift.isNotBlank()
    val hasLocation get() = latitude != null && longitude != null
    val isBlank get() = editingId == null && type.isBlank() && process.isBlank() && area.isBlank() &&
        shift.isBlank() && notes.isBlank() && attachments.isEmpty() && !hasLocation
}

/**
 * Estado único de la app (flujo unidireccional): las pantallas leen estado de Compose y emiten
 * eventos llamando a funciones del ViewModel. Room es la fuente de verdad; [reports] es su copia
 * observable, actualizada de forma optimista mientras hay escrituras pendientes.
 * La API de envío sigue simulada (mock).
 */
class ReportsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ReportRepository(AppDatabase.get(app))
    private val store = SettingsStore(app)
    private val connectivity = ConnectivityObserver(app)

    var reports by mutableStateOf<List<Report>>(emptyList())
        private set
    /** false mientras se abre la base de datos y se lee la sesión guardada. */
    var ready by mutableStateOf(false)
        private set
    var user by mutableStateOf<DemoUser?>(null)
        private set
    var demoOffline by mutableStateOf(false)
        private set
    var simulateServerError by mutableStateOf(false)
        private set
    var draft by mutableStateOf(Draft())
        private set

    private var networkAvailable by mutableStateOf(true)
    val isOnline get() = networkAvailable && !demoOffline

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    private var nextNumber = 13L
    private var nextAttachment = 100
    private val draftAddedPaths = mutableSetOf<String>()
    private val syncJobs = mutableMapOf<String, Job>()

    // Escrituras a Room en orden; mientras haya pendientes se ignoran emisiones antiguas de la base.
    private val writeLock = Mutex()
    private var pendingWrites = 0
    private var latest: List<Report>? = null

    private val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    private fun now() = LocalDateTime.now().format(fmt)
    private fun shortNow() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))

    init {
        viewModelScope.launch {
            repo.resetSending()
            repo.seedIfEmpty(MockData.initialReports())
            val s = store.load()
            demoOffline = s.demoOffline
            simulateServerError = s.simulateServerError
            user = MockData.users.firstOrNull { it.email == s.sessionEmail }
            networkAvailable = connectivity.isOnlineNow()
            ready = true
            launch {
                repo.observeAll().collect {
                    latest = it
                    if (pendingWrites == 0) applyList(it)
                }
            }
            launch { connectivity.online.collect { updateOnline(network = it) } }
        }
    }

    private fun applyList(list: List<Report>) {
        reports = list
        nextNumber = maxOf(nextNumber, (list.maxOfOrNull { ReportRepository.numberOf(it.id) } ?: 0L) + 1)
    }

    private fun write(block: suspend () -> Unit) {
        pendingWrites++
        viewModelScope.launch {
            try {
                writeLock.withLock { block() }
            } finally {
                pendingWrites--
                if (pendingWrites == 0) latest?.let { applyList(it) }
            }
        }
    }

    private fun persist(r: Report) {
        reports = if (reports.any { it.id == r.id }) reports.map { if (it.id == r.id) r else it } else listOf(r) + reports
        write { repo.save(r) }
    }

    private fun commit(id: String, change: (Report) -> Report) {
        val current = report(id) ?: return
        persist(change(current))
    }

    private fun Report.withEvent(text: String) = copy(timeline = timeline + TimelineEvent(shortNow(), text))

    fun report(id: String?): Report? = reports.firstOrNull { it.id == id }

    val pendingCount get() = reports.count { it.status == ReportStatus.EN_COLA || it.status == ReportStatus.ENVIANDO }

    // ---- Sesión simulada ----
    fun login(email: String, password: String): Boolean {
        val found = MockData.users.firstOrNull { it.email.equals(email.trim(), ignoreCase = true) }
        if (found != null && password == MockData.DEMO_PASSWORD) {
            startSession(found)
            return true
        }
        return false
    }

    fun loginAs(demo: DemoUser) = startSession(demo)

    private fun startSession(demo: DemoUser) {
        user = demo
        viewModelScope.launch { store.setSession(demo.email) }
    }

    fun logout() {
        discardDraft()
        user = null
        viewModelScope.launch { store.setSession(null) }
    }

    // ---- Borrador ----
    fun startNewDraft() {
        discardDraft()
    }

    fun startEdit(report: Report) {
        discardDraft()
        draft = Draft(
            editingId = report.id, type = report.type, process = report.process, area = report.area,
            shift = report.shift, notes = report.notes, attachments = report.attachments,
            latitude = report.latitude, longitude = report.longitude,
        )
    }

    /** Descarta el borrador en edición y los archivos que solo existían en él. */
    fun discardDraft() {
        draftAddedPaths.forEach { ImageUtils.deleteQuietly(it) }
        draftAddedPaths.clear()
        draft = Draft()
    }

    fun updateDraft(transform: (Draft) -> Draft) {
        draft = transform(draft)
    }

    fun setLocation(latitude: Double?, longitude: Double?) {
        draft = draft.copy(latitude = latitude, longitude = longitude)
    }

    /** Valida formato y tamaño; devuelve el mensaje de error o null si el archivo es aceptable. */
    fun validateAttachment(name: String, sizeMb: Double, replaceId: String? = null): String? {
        val ext = name.substringAfterLast('.', "").lowercase()
        if (ext !in setOf("jpg", "jpeg", "png", "pdf")) return "Formato no permitido (.$ext). Use JPG, PNG o PDF."
        if (sizeMb <= 0.0) return "El archivo está vacío."
        if (sizeMb > MockData.MAX_FILE_MB) return "El archivo supera el máximo de 10 MB."
        if (replaceId == null && draft.attachments.size >= MockData.MAX_ATTACHMENTS) {
            return "Máximo ${MockData.MAX_ATTACHMENTS} archivos por reporte."
        }
        return null
    }

    /** Agrega (o reemplaza) un adjunto. Devuelve el mensaje de error o null si se agregó. */
    fun addAttachment(name: String, sizeMb: Double, replaceId: String? = null, path: String = ""): String? {
        validateAttachment(name, sizeMb, replaceId)?.let { return it }
        val kind = if (name.substringAfterLast('.').equals("pdf", ignoreCase = true)) AttachmentKind.PDF else AttachmentKind.IMAGEN
        val item = Attachment("N${nextAttachment++}", name, sizeMb, kind, path)
        if (path.isNotBlank()) draftAddedPaths += path
        if (replaceId != null) releaseFile(draft.attachments.firstOrNull { it.id == replaceId })
        draft = draft.copy(
            attachments = if (replaceId == null) draft.attachments + item
            else draft.attachments.map { if (it.id == replaceId) item else it },
        )
        return null
    }

    fun removeAttachment(id: String) {
        releaseFile(draft.attachments.firstOrNull { it.id == id })
        draft = draft.copy(attachments = draft.attachments.filterNot { it.id == id })
    }

    /** Solo borra el archivo si se creó en este borrador (los de un reporte guardado se conservan). */
    private fun releaseFile(a: Attachment?) {
        if (a != null && draftAddedPaths.remove(a.path)) ImageUtils.deleteQuietly(a.path)
    }

    // ---- Guardado y cola ----
    /** Guarda el borrador actual. Con [send] pasa a la cola y se intenta sincronizar. Devuelve el id. */
    fun saveDraft(send: Boolean): String {
        val d = draft
        val existing = report(d.editingId)
        val status = if (send) ReportStatus.EN_COLA else ReportStatus.BORRADOR
        val event = when {
            existing != null && send -> "Editado y puesto en cola"
            existing != null -> "Editado (borrador)"
            send -> "Reporte creado y puesto en cola (guardado local)"
            else -> "Borrador creado en el dispositivo"
        }
        val saved = if (existing != null) {
            existing.copy(
                type = d.type, process = d.process, area = d.area, shift = d.shift, notes = d.notes,
                attachments = d.attachments, status = status, supervisorNote = null,
                latitude = d.latitude, longitude = d.longitude,
            ).withEvent(event)
        } else {
            Report(
                id = "RPT-2026-%04d".format(nextNumber++), type = d.type, process = d.process,
                area = d.area, shift = d.shift, notes = d.notes, user = user?.name ?: "Usuario demo",
                createdAt = now(), status = status, attempts = 0, attachments = d.attachments,
                timeline = listOf(TimelineEvent(shortNow(), event)),
                latitude = d.latitude, longitude = d.longitude,
            )
        }
        // Archivos del reporte anterior que el usuario quitó al editar.
        existing?.attachments?.filter { old -> d.attachments.none { it.path == old.path } }?.forEach { ImageUtils.deleteQuietly(it.path) }
        draftAddedPaths.clear()
        persist(saved)
        draft = Draft()
        if (send) sync(saved.id, automatic = true) else _messages.tryEmit("Borrador guardado en el teléfono")
        return saved.id
    }

    private suspend fun attemptSend(id: String, last: Boolean): Boolean {
        commit(id) { it.copy(status = ReportStatus.ENVIANDO).withEvent("Enviando a VITAIROS (mock)") }
        delay(1800)
        val current = report(id) ?: return true // eliminado durante el envío
        return if (simulateServerError) {
            commit(id) {
                it.copy(status = if (last) ReportStatus.ERROR else ReportStatus.EN_COLA, attempts = it.attempts + 1)
                    .withEvent("Intento ${it.attempts + 1} fallido: error del servidor simulado")
            }
            if (last) _messages.tryEmit("Error al enviar ${current.id}. Puede reintentar manualmente")
            false
        } else {
            commit(id) { it.copy(status = ReportStatus.SINCRONIZADO, attempts = it.attempts + 1).withEvent("Sincronizado correctamente") }
            _messages.tryEmit("${current.id} enviado correctamente")
            true
        }
    }

    /**
     * Simula el envío al mock de la API. Automático: hasta 3 intentos con espera de 5, 15 y 30 s.
     * Manual (botón Reintentar): un intento inmediato.
     */
    fun sync(id: String, automatic: Boolean = false) {
        val current = report(id) ?: return
        if (current.status == ReportStatus.ENVIANDO || current.status == ReportStatus.SINCRONIZADO) return
        if (syncJobs[id]?.isActive == true) return
        syncJobs[id] = viewModelScope.launch {
            val attempts = if (automatic) MockData.retryBackoffSeconds.size else 1
            for (i in 0 until attempts) {
                if (report(id) == null) return@launch
                if (!isOnline) {
                    commit(id) { it.copy(status = ReportStatus.EN_COLA).withEvent("Sin conexión: permanece en cola") }
                    _messages.tryEmit("Sin conexión. $id queda en cola y se enviará al recuperar la red")
                    return@launch
                }
                val last = i == attempts - 1
                if (attemptSend(id, last)) return@launch
                if (!last) {
                    val wait = MockData.retryBackoffSeconds[i]
                    commit(id) { it.withEvent("Reintento automático ${i + 2} de $attempts en $wait s") }
                    delay(wait * 1000L)
                }
            }
        }
    }

    fun syncAll() {
        val ids = reports.filter { it.status == ReportStatus.EN_COLA || it.status == ReportStatus.ERROR }.map { it.id }
        if (ids.isEmpty()) _messages.tryEmit("No hay reportes pendientes")
        ids.forEach { sync(it) }
    }

    fun delete(id: String) {
        val r = report(id) ?: return
        syncJobs.remove(id)?.cancel()
        r.attachments.forEach { ImageUtils.deleteQuietly(it.path) }
        reports = reports.filterNot { it.id == id }
        write { repo.delete(id) }
        _messages.tryEmit("Reporte $id eliminado")
    }

    // ---- Conectividad y preferencias ----
    private fun updateOnline(network: Boolean = networkAvailable, demo: Boolean = demoOffline) {
        val before = isOnline
        networkAvailable = network
        demoOffline = demo
        if (!before && isOnline) {
            _messages.tryEmit("Conexión recuperada. Sincronizando pendientes…")
            reports.filter { it.status == ReportStatus.EN_COLA }.forEach { sync(it.id, automatic = true) }
        } else if (before && !isOnline) {
            _messages.tryEmit("Sin conexión. Los reportes se guardan en el teléfono")
        }
    }

    fun changeDemoOffline(value: Boolean) {
        updateOnline(demo = value)
        viewModelScope.launch { store.setDemoOffline(value) }
    }

    fun setSimulateError(value: Boolean) {
        simulateServerError = value
        viewModelScope.launch { store.setSimulateError(value) }
    }
}
