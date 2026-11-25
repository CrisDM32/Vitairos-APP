package com.vitairos.industrialai.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import com.vitairos.industrialai.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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

class ReportsViewModel(private val app: Application) : AndroidViewModel(app) {
    private val api = Backend()
    private val store = SettingsStore(app)
    private val connectivity = ConnectivityObserver(app)
    private lateinit var repo: ReportRepository
    private var session: Session? = null
    private val disk = Mutex()
    private val remote = Mutex()
    private var accountJob = SupervisorJob(viewModelScope.coroutineContext[Job])
    private val accountScope get() = CoroutineScope(viewModelScope.coroutineContext + accountJob)
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()
    private var records by mutableStateOf<List<Report>>(emptyList())
    val reports get() = records.filterNot { it.deleted }
    var ready by mutableStateOf(false)
        private set
    var user by mutableStateOf<DemoUser?>(null)
        private set
    var loggingIn by mutableStateOf(false)
        private set
    var demoOffline by mutableStateOf(false)
        private set
    private var networkAvailable by mutableStateOf(true)
    val isOnline get() = networkAvailable && !demoOffline
    private val defaults = Catalog(MockData.reportTypes, MockData.processes, MockData.areas, MockData.shifts)
    var catalog by mutableStateOf(defaults)
        private set
    var draft by mutableStateOf(Draft())
        private set
    private val draftAddedPaths = mutableSetOf<String>()
    private val syncJobs = mutableMapOf<String, Job>()
    private val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    private fun now() = LocalDateTime.now().format(fmt)
    private fun shortNow() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))

    init {
        viewModelScope.launch {
            demoOffline = store.offline()
            networkAvailable = connectivity.isOnlineNow()
            val saved = store.session()
            if (saved != null && Instant.parse(saved.expiresAt).isAfter(Instant.now())) activate(saved)
            ready = true
            connectivity.online.collect { updateOnline(network = it) }
        }
    }

    fun login(email: String, password: String, done: (String?) -> Unit) {
        if (loggingIn) return
        loggingIn = true
        viewModelScope.launch {
            try {
                val s = api.login(email,password)
                store.setSession(s)
                activate(s)
                done(null)
            } catch (e: Exception) {
                done(if (e is ApiError && e.code == 401) "Correo o contraseña incorrectos" else "No se pudo iniciar sesión: ${e.localizedMessage}")
            } finally { loggingIn = false }
        }
    }

    private suspend fun activate(s: Session) {
        val dbName = "reports-" + UUID.nameUUIDFromBytes(s.profile.email.toByteArray()) + ".db"
        repo = ReportRepository(AppDatabase.get(app, dbName))
        disk.withLock {
            repo.resetSending()
            // Recupera reportes reales del usuario de la antigua base compartida de demostración.
            if (app.getDatabasePath("vitairos.db").exists()) {
                val legacy = ReportRepository(AppDatabase.get(app))
                val examples = MockData.initialReports().associateBy { it.id }
                val imported = repo.observeAll().first().map { it.id }.toSet()
                for (r in legacy.observeAll().first().filter { it.user == s.profile.name }) {
                    if (r == examples[r.id]) continue
                    val id = "LOCAL-" + UUID.nameUUIDFromBytes((s.profile.email + r.id).toByteArray())
                    if (id !in imported) repo.save(r.copy(id=id,status=ReportStatus.BORRADOR,dirty=false))
                    legacy.delete(r.id)
                }
            }
            records = repo.observeAll().first().sortedByDescending { LocalDateTime.parse(it.createdAt,fmt) }
        }
        session = s
        user = s.profile
        accountScope.launch {
            delay((Instant.parse(s.expiresAt).toEpochMilli() - System.currentTimeMillis()).coerceAtLeast(0))
            if (session == s) logout()
        }
        refresh()
    }

    fun logout() {
        val previous = session ?: return
        accountJob.cancel()
        accountJob = SupervisorJob(viewModelScope.coroutineContext[Job])
        syncJobs.clear()
        discardDraft()
        session = null
        user = null
        records = emptyList()
        catalog = defaults
        viewModelScope.launch {
            store.setSession(null)
            try { api.logout(previous.token) } catch (_: java.io.IOException) { }
        }
    }

    private fun problem(e: Exception) {
        if (e is CancellationException) throw e
        if (e is ApiError && e.code == 401) {
            logout()
            _messages.tryEmit("La sesión terminó. Ingrese nuevamente.")
        } else _messages.tryEmit("No se pudo completar la operación: ${e.localizedMessage}")
    }
    private fun write(block: suspend (ReportRepository) -> Unit) {
        val target = repo
        viewModelScope.launch { disk.withLock { block(target) } }
    }
    private fun persist(r: Report) {
        records = if (records.any { it.id == r.id }) records.map { if (it.id == r.id) r else it } else listOf(r) + records
        write { it.save(r) }
    }
    private fun forget(r: Report) {
        records = records.filterNot { it.id == r.id }
        write { it.delete(r.id) }
        r.attachments.forEach { ImageUtils.deleteQuietly(it.path) }
    }
    private fun Report.withEvent(text: String) = copy(timeline = timeline + TimelineEvent(shortNow(), text))
    fun report(id: String?) = reports.firstOrNull { it.id == id }
    val pendingCount get() = reports.count { it.status == ReportStatus.EN_COLA || it.status == ReportStatus.ENVIANDO }

    fun refresh() {
        val s = session ?: return
        if (!isOnline) return
        accountScope.launch {
            try {
                remote.withLock {
                    api.me(s.token)
                    catalog = api.catalog(s.token)
                    val ids = api.ids(s.token)
                    for (id in ids.asReversed()) {
                        val old = records.find { it.id == id }
                        if (old?.dirty == true || old?.deleted == true) continue
                        val updated = api.detail(s.token,id,old)
                        if (records.find { it.id == id } == old) persist(updated)
                    }
                    records.filter { !it.dirty && it.id !in ids && !(it.id.startsWith("LOCAL-") && it.status == ReportStatus.BORRADOR) }.forEach(::forget)
                }
            } catch (e: Exception) { problem(e) }
            if (s == session) syncAll()
        }
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
    fun validateAttachment(name: String, sizeMb: Double, replaceId: String? = null): String? =
        Validators.attachment(name, sizeMb, draft.attachments.size, replacing = replaceId != null)

    /** Agrega (o reemplaza) un adjunto. Devuelve el mensaje de error o null si se agregó. */
    fun addAttachment(name: String, sizeMb: Double, replaceId: String? = null, path: String = ""): String? {
        validateAttachment(name, sizeMb, replaceId)?.let { return it }
        val kind = if (name.substringAfterLast('.').equals("pdf", ignoreCase = true)) AttachmentKind.PDF else AttachmentKind.IMAGEN
        val item = Attachment(UUID.randomUUID().toString(), name, sizeMb, kind, path)
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
                id = "RPT-" + UUID.randomUUID(), type = d.type, process = d.process,
                area = d.area, shift = d.shift, notes = d.notes, user = user?.name ?: "Usuario demo",
                createdAt = now(), status = status, attempts = 0, attachments = d.attachments,
                timeline = listOf(TimelineEvent(shortNow(), event)),
                latitude = d.latitude, longitude = d.longitude,
            )
        }
        // Archivos del reporte anterior que el usuario quitó al editar.
        existing?.attachments?.filter { old -> d.attachments.none { it.path == old.path } }?.forEach { ImageUtils.deleteQuietly(it.path) }
        draftAddedPaths.clear()
        persist(saved.copy(dirty=true))
        draft = Draft()
        syncAfterChange(saved.id)
        return saved.id
    }

    private fun syncAfterChange(id: String) {
        val active = syncJobs[id]
        accountScope.launch {
            active?.join()
            if (isOnline) sync(id, automatic = true)
        }
    }

    fun sync(id: String, automatic: Boolean = false) {
        val s = session ?: return
        if (!isOnline || syncJobs[id]?.isActive == true) return
        syncJobs[id] = accountScope.launch {
            val tries = if (automatic) MockData.retryBackoffSeconds.size else 1
            for (attempt in 0 until tries) {
                if (!isOnline) return@launch
                var retry = false
                remote.withLock {
                    val r = records.find { it.id == id } ?: return@withLock
                    if (!r.dirty && !r.deleted) return@withLock
                    val sending = !r.deleted && r.status != ReportStatus.BORRADOR
                    val current = if (sending) r.copy(status=ReportStatus.ENVIANDO) else r
                    if (sending) persist(current)
                    try {
                        disk.withLock { } // El borrador ya está en Room antes de iniciar la petición.
                        if (r.deleted) {
                            api.delete(s.token,id)
                            forget(r)
                        } else {
                            val saved = api.save(s.token,current,sending)
                            if (records.find { it.id == id } == current) persist(saved)
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        val transient = e !is ApiError || e.code >= 500
                        retry = transient && attempt + 1 < tries
                        if (sending && report(id) == current) persist(current.copy(
                            status=if (retry || !isOnline) ReportStatus.EN_COLA else ReportStatus.ERROR,
                            attempts=current.attempts+1).withEvent("Error de envío: ${e.localizedMessage}"))
                        if (!retry || e is ApiError && e.code == 401) problem(e)
                        if (r.deleted && e is ApiError && e.code == 409) {
                            persist(r.copy(deleted=false,dirty=false))
                            refresh()
                        }
                    }
                }
                if (!retry) return@launch
                delay(MockData.retryBackoffSeconds[attempt] * 1000L)
            }
        }
    }
    fun syncAll() = records.filter { it.dirty || it.deleted }.forEach { sync(it.id,automatic=true) }

    fun delete(id: String) {
        val r = report(id) ?: return
        if (r.user != user?.name || !r.status.deletable) return
        persist(r.copy(deleted=true,dirty=true))
        syncAfterChange(id)
    }

    fun correct(r: Report, note: String) {
        val s = session ?: return
        accountScope.launch {
            try { remote.withLock { persist(api.correct(s.token,r,note.trim())) } }
            catch (e: Exception) { problem(e) }
        }
    }

    suspend fun currentWeather(latitude: Double, longitude: Double): org.json.JSONObject? {
        val s = session ?: return null
        return try { api.currentWeather(s.token,latitude,longitude).takeIf { session == s } }
        catch (e: Exception) { if (session == s) problem(e); null }
    }

    fun download(id: String, attachment: Attachment) {
        val s = session ?: return
        accountScope.launch {
            try {
                remote.withLock {
                    val current = report(id) ?: return@withLock
                    val file = api.download(s.token,id,attachment,ImageUtils.attachmentsDir(app))
                    if (report(id) == current) persist(current.copy(attachments=current.attachments.map {
                        if (it.id == attachment.id) it.copy(path=file.path) else it
                    })) else file.delete()
                }
            } catch (e: Exception) { problem(e) }
        }
    }

    private fun updateOnline(network: Boolean = networkAvailable, demo: Boolean = demoOffline) {
        val before = isOnline
        networkAvailable = network
        demoOffline = demo
        if (!before && isOnline) refresh()
    }

    fun changeDemoOffline(value: Boolean) {
        updateOnline(demo = value)
        viewModelScope.launch { store.setDemoOffline(value) }
    }
}
