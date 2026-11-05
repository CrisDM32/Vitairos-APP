package com.vitairos.industrialai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vitairos.industrialai.data.MockData
import com.vitairos.industrialai.model.ReportStatus
import com.vitairos.industrialai.ui.ReportFormErrors
import com.vitairos.industrialai.ui.ReportsViewModel
import com.vitairos.industrialai.ui.Validators
import com.vitairos.industrialai.ui.components.Appear
import com.vitairos.industrialai.ui.components.DropdownField
import com.vitairos.industrialai.ui.components.InfoRow
import com.vitairos.industrialai.ui.components.PrimaryButton
import com.vitairos.industrialai.ui.components.SecondaryButton
import com.vitairos.industrialai.ui.components.SectionTitle
import com.vitairos.industrialai.ui.components.StatusChip
import com.vitairos.industrialai.ui.components.VitairosTopBar
import com.vitairos.industrialai.util.GpsResult
import com.vitairos.industrialai.util.LocationHelper
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun StepHeader(step: Int, title: String) {
    val progress by animateFloatAsState(step / 3f, tween(500), label = "stepProgress")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Paso $step de 3 · $title", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

fun formatCoords(lat: Double, lon: Double) = "%.5f, %.5f".format(lat, lon)

@Composable
fun NewReportScreen(vm: ReportsViewModel, onBack: () -> Unit, onContinue: () -> Unit) {
    val d = vm.draft
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showErrors by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    var gpsMessage by remember { mutableStateOf<String?>(null) }
    val errors = if (showErrors) ReportFormErrors.validate(d) else ReportFormErrors()

    fun requestBack() = if (d.isBlank) onBack() else confirmDiscard = true
    BackHandler(enabled = !d.isBlank) { confirmDiscard = true }

    fun fetchLocation() {
        scope.launch {
            locating = true
            gpsMessage = null
            when (val r = LocationHelper.current(context)) {
                is GpsResult.Success -> {
                    vm.setLocation(r.latitude, r.longitude)
                    gpsMessage = "Ubicación capturada (±${r.accuracyMeters.toInt()} m)"
                }
                GpsResult.NoPermission -> gpsMessage = "Sin permiso de ubicación. Puede habilitarlo en Ajustes del teléfono."
                GpsResult.Disabled -> gpsMessage = "El GPS está desactivado. Actívelo e intente de nuevo."
                GpsResult.Timeout -> gpsMessage = "No se pudo obtener la ubicación. Salga a un lugar abierto e intente de nuevo."
            }
            locating = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) fetchLocation()
        else gpsMessage = "Permiso de ubicación denegado. La ubicación es opcional: puede continuar sin ella."
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        VitairosTopBar(if (d.editingId != null) "Editar reporte" else "Nuevo reporte", onBack = ::requestBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StepHeader(1, "Datos del reporte")

            Appear(0) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Se completa automáticamente", style = MaterialTheme.typography.labelLarge)
                        InfoRow("Identificador", d.editingId ?: "Se asigna al guardar")
                        InfoRow("Fecha y hora", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                        InfoRow("Usuario", vm.user?.name ?: "Usuario demo")
                        Row { StatusChip(ReportStatus.BORRADOR) }
                    }
                }
            }

            SectionTitle("Tipo y proceso")
            DropdownField("Tipo de reporte *", d.type, MockData.reportTypes, { v -> vm.updateDraft { it.copy(type = v) } }, error = errors.type)
            DropdownField("Proceso asociado *", d.process, MockData.processes, { v -> vm.updateDraft { it.copy(process = v) } }, error = errors.process)
            DropdownField("Área / faena *", d.area, MockData.areas, { v -> vm.updateDraft { it.copy(area = v) } }, error = errors.area)
            DropdownField("Turno *", d.shift, MockData.shifts, { v -> vm.updateDraft { it.copy(shift = v) } }, error = errors.shift)

            val incident = d.type.contains("incidente", ignoreCase = true)
            OutlinedTextField(
                value = d.notes,
                onValueChange = { v -> if (v.length <= Validators.NOTES_MAX) vm.updateDraft { it.copy(notes = v) } },
                label = { Text(if (incident) "Observaciones *" else "Observaciones (opcional)") },
                minLines = 3,
                isError = errors.notes != null,
                supportingText = {
                    Row(Modifier.fillMaxWidth()) {
                        Text(errors.notes.orEmpty(), Modifier.weight(1f))
                        Text("${d.notes.length}/${Validators.NOTES_MAX}")
                    }
                },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            )

            SectionTitle("Ubicación (opcional)")
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (d.latitude != null && d.longitude != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Icon(Icons.Filled.LocationOn, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text(formatCoords(d.latitude, d.longitude), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        }
                    } else {
                        Text("Registra dónde se generó el reporte.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        SecondaryButton(
                            if (d.hasLocation) "Actualizar" else "Capturar ubicación",
                            onClick = {
                                if (LocationHelper.hasPermission(context)) fetchLocation()
                                else permissionLauncher.launch(LocationHelper.permissions)
                            },
                            Modifier.weight(1f), Icons.Filled.LocationOn, enabled = !locating,
                        )
                        if (d.hasLocation) SecondaryButton("Quitar", { vm.setLocation(null, null); gpsMessage = null }, icon = Icons.Filled.Close)
                    }
                    AnimatedVisibility(locating) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("  Obteniendo ubicación…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    AnimatedVisibility(gpsMessage != null) {
                        Text(gpsMessage.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text("* Obligatorio. Tipo y proceso se eligen de listas fijas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.padding(16.dp)) {
            PrimaryButton("Continuar: adjuntar documento", onClick = {
                showErrors = true
                if (!ReportFormErrors.validate(d).hasErrors) onContinue()
            })
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("¿Descartar cambios?") },
            text = { Text("Los datos y adjuntos no guardados de este reporte se perderán.") },
            confirmButton = { TextButton({ confirmDiscard = false; vm.discardDraft(); onBack() }) { Text("Descartar") } },
            dismissButton = { TextButton({ confirmDiscard = false }) { Text("Seguir editando") } },
        )
    }
}
