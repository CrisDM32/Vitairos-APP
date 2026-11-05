package com.vitairos.industrialai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vitairos.industrialai.model.Report
import com.vitairos.industrialai.model.ReportStatus
import com.vitairos.industrialai.ui.ReportsViewModel
import androidx.compose.foundation.layout.height
import com.vitairos.industrialai.ui.components.AttachmentPreview
import com.vitairos.industrialai.ui.components.InfoRow
import com.vitairos.industrialai.ui.components.PrimaryButton
import com.vitairos.industrialai.ui.components.SecondaryButton
import com.vitairos.industrialai.ui.components.SectionTitle
import com.vitairos.industrialai.ui.components.StatusChip
import com.vitairos.industrialai.ui.components.VitairosTopBar

@Composable
fun ReportDetailScreen(vm: ReportsViewModel, reportId: String, onBack: () -> Unit, onEdit: (Report) -> Unit) {
    val r = vm.report(reportId)
    var confirmDelete by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        VitairosTopBar(reportId, onBack = onBack)
        if (r == null) {
            Text("El reporte ya no existe.", modifier = Modifier.padding(16.dp))
            return@Column
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row { StatusChip(r.status) }
            Text(r.status.hint, style = MaterialTheme.typography.bodyLarge)
            if (r.status == ReportStatus.ENVIANDO) LinearProgressIndicator(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
            r.supervisorNote?.let {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Observación del supervisor (simulada)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(14.dp)) {
                    InfoRow("Tipo de reporte", r.type)
                    InfoRow("Proceso asociado", r.process)
                    InfoRow("Área / faena", r.area)
                    InfoRow("Turno", r.shift)
                    InfoRow("Usuario", r.user)
                    InfoRow("Fecha y hora de creación", r.createdAt)
                    InfoRow("Intentos de envío", r.attempts.toString())
                    InfoRow("Observaciones", r.notes)
                    InfoRow("Ubicación", if (r.latitude != null && r.longitude != null) formatCoords(r.latitude, r.longitude) else "No registrada")
                }
            }
            SectionTitle("Adjuntos")
            r.attachments.forEach { a ->
                Text("• ${a.name} · ${"%.1f".format(a.sizeMb)} MB", style = MaterialTheme.typography.bodyLarge)
                if (a.path.isNotBlank()) AttachmentPreview(a, Modifier.fillMaxWidth(0.6f).height(220.dp).align(Alignment.CenterHorizontally))
            }

            SectionTitle("Trazabilidad")
            r.timeline.forEach { e ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.padding(top = 6.dp).size(10.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)))
                    Column(Modifier.padding(start = 12.dp, bottom = 8.dp)) {
                        Text(e.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(e.text, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            if (r.status == ReportStatus.SINCRONIZADO) {
                Text(
                    "Reporte sincronizado: se conserva como registro de trazabilidad y no puede editarse ni eliminarse.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (r.status != ReportStatus.SINCRONIZADO && r.status != ReportStatus.ENVIANDO) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (r.status) {
                    ReportStatus.REQUIERE_CORRECCION -> PrimaryButton("Corregir y reenviar", { onEdit(r) }, icon = Icons.Filled.Edit)
                    ReportStatus.BORRADOR -> PrimaryButton("Continuar edición", { onEdit(r) }, icon = Icons.Filled.Edit)
                    else -> PrimaryButton("Reintentar envío", { vm.sync(r.id) }, icon = Icons.Filled.Refresh)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (r.status == ReportStatus.EN_COLA || r.status == ReportStatus.ERROR) {
                        SecondaryButton("Editar", { onEdit(r) }, Modifier.weight(1f), Icons.Filled.Edit)
                    }
                    if (r.status.deletable) {
                        SecondaryButton("Eliminar", { confirmDelete = true }, Modifier.weight(1f), Icons.Filled.Delete)
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar reporte?") },
            text = { Text("$reportId aún no se ha sincronizado. Se perderá su contenido.") },
            confirmButton = { TextButton({ confirmDelete = false; vm.delete(reportId); onBack() }) { Text("Eliminar") } },
            dismissButton = { TextButton({ confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}
