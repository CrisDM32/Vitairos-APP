package com.vitairos.industrialai.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

/** Estados visibles del reporte. Siempre se muestran con texto + icono, nunca solo con color. */
enum class ReportStatus(val label: String, val icon: ImageVector, val hint: String) {
    BORRADOR("Borrador", Icons.Filled.Edit, "Guardado en el teléfono, aún editable"),
    EN_COLA("En cola", Icons.AutoMirrored.Filled.List, "Esperando conexión para sincronizar"),
    ENVIANDO("Enviando", Icons.Filled.Refresh, "Sincronización en curso"),
    SINCRONIZADO("Sincronizado", Icons.Filled.CheckCircle, "Recibido por VITAIROS"),
    ERROR("Error", Icons.Filled.Warning, "No se pudo enviar. Reintente"),
    REQUIERE_CORRECCION("Requiere corrección", Icons.Filled.Info, "Observado: corrija y reenvíe");

    /** Aún puede eliminarse (no ha sido sincronizado ni está en envío). */
    val deletable get() = this == BORRADOR || this == EN_COLA || this == ERROR

    /** Pertenece a la cola de envíos. */
    val inQueue get() = this == BORRADOR || this == EN_COLA || this == ENVIANDO || this == ERROR ||
        this == REQUIERE_CORRECCION
}

enum class AttachmentKind { IMAGEN, PDF }

data class Attachment(
    val id: String,
    val name: String,
    val sizeMb: Double,
    val kind: AttachmentKind,
    /** Ruta del archivo en almacenamiento privado; vacía en los datos de ejemplo. */
    val path: String = "",
)

data class TimelineEvent(val time: String, val text: String)

data class Report(
    val id: String,
    val type: String,
    val process: String,
    val area: String,
    val shift: String,
    val notes: String,
    val user: String,
    val createdAt: String,
    val status: ReportStatus,
    val attempts: Int,
    val attachments: List<Attachment>,
    val timeline: List<TimelineEvent>,
    val supervisorNote: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    val hasLocation get() = latitude != null && longitude != null
}

enum class Role(val label: String) { OPERADOR("Operador"), SUPERVISOR("Supervisor") }

data class DemoUser(val email: String, val name: String, val role: Role)

/** Campo extraído por la simulación de VITAIROS Industrial AI. */
data class ExtractedField(val label: String, val value: String)
