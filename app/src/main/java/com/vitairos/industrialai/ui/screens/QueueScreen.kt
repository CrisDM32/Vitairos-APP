package com.vitairos.industrialai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.Alignment
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vitairos.industrialai.data.MockData
import com.vitairos.industrialai.model.Report
import com.vitairos.industrialai.model.ReportStatus
import com.vitairos.industrialai.ui.ReportsViewModel
import com.vitairos.industrialai.ui.components.ConnectionBanner
import com.vitairos.industrialai.ui.components.PrimaryButton
import com.vitairos.industrialai.ui.components.ReportCard
import com.vitairos.industrialai.ui.components.SecondaryButton
import com.vitairos.industrialai.ui.components.VitairosTopBar

@Composable
fun QueueScreen(vm: ReportsViewModel, onOpenReport: (String) -> Unit, onEdit: (Report) -> Unit) {
    val queue = vm.reports.filter { it.status.inQueue }
    var toDelete by remember { mutableStateOf<Report?>(null) }

    Column(Modifier.fillMaxSize()) {
        VitairosTopBar("Cola de envíos")
        ConnectionBanner(vm.isOnline, vm.pendingCount)
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Text(
                        "Sincronización automática al recuperar conexión: hasta 3 reintentos (${MockData.retryBackoffSeconds.joinToString(", ") { "$it s" }}). Luego use Reintentar.",
                        modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item { PrimaryButton("Sincronizar todo", onClick = { vm.syncAll() }, icon = Icons.Filled.Refresh) }
            if (queue.isEmpty()) {
                item { Text("No hay reportes pendientes.", style = MaterialTheme.typography.bodyLarge) }
            }
            items(queue, key = { it.id }) { r ->
                val swipe = rememberSwipeToDismissBoxState()
                // La tarjeta vuelve a su sitio; la eliminación se confirma en el diálogo.
                LaunchedEffect(swipe.currentValue) {
                    if (swipe.currentValue == SwipeToDismissBoxValue.EndToStart) {
                        toDelete = r
                        swipe.reset()
                    }
                }
                SwipeToDismissBox(
                    state = swipe,
                    modifier = Modifier.animateItem(),
                    enableDismissFromStartToEnd = false,
                    enableDismissFromEndToStart = r.status.deletable,
                    backgroundContent = {
                        Row(
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.error, RoundedCornerShape(14.dp)).padding(end = 20.dp),
                            horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onError)
                            Text(" Eliminar", color = MaterialTheme.colorScheme.onError, style = MaterialTheme.typography.labelLarge)
                        }
                    },
                ) {
                ReportCard(r, onClick = { onOpenReport(r.id) }) {
                    if (r.status == ReportStatus.ENVIANDO) {
                        LinearProgressIndicator(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                    }
                    if (r.status == ReportStatus.ERROR) {
                        Text("Reintentos automáticos agotados (${r.attempts}).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    if (r.status != ReportStatus.ENVIANDO) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (r.status != ReportStatus.BORRADOR && r.status != ReportStatus.REQUIERE_CORRECCION) {
                                SecondaryButton("Reintentar", { vm.sync(r.id) }, Modifier.weight(1f), Icons.Filled.Refresh)
                            } else {
                                SecondaryButton("Editar", { onEdit(r) }, Modifier.weight(1f), Icons.Filled.Edit)
                            }
                            if (r.status.deletable) {
                                SecondaryButton("Eliminar", { toDelete = r }, Modifier.weight(1f), Icons.Filled.Delete)
                            }
                        }
                    }
                }
                }
            }
        }
    }

    toDelete?.let { r ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("¿Eliminar reporte?") },
            text = { Text("${r.id} aún no se ha sincronizado. Se perderá su contenido y no se podrá recuperar.") },
            confirmButton = { TextButton({ vm.delete(r.id); toDelete = null }) { Text("Eliminar") } },
            dismissButton = { TextButton({ toDelete = null }) { Text("Cancelar") } },
        )
    }
}
