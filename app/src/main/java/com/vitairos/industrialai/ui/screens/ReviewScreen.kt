package com.vitairos.industrialai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vitairos.industrialai.ui.ReportsViewModel
import com.vitairos.industrialai.ui.components.InfoRow
import com.vitairos.industrialai.ui.components.PrimaryButton
import com.vitairos.industrialai.ui.components.SecondaryButton
import com.vitairos.industrialai.ui.components.SectionTitle
import com.vitairos.industrialai.ui.components.SimulationBadge
import com.vitairos.industrialai.ui.components.VitairosTopBar

@Composable
fun ReviewScreen(vm: ReportsViewModel, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val d = vm.draft
    Column(Modifier.fillMaxSize()) {
        VitairosTopBar("Revisar y confirmar", onBack = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StepHeader(3, "Revisión")
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(14.dp)) {
                    InfoRow("Tipo de reporte", d.type)
                    InfoRow("Proceso asociado", d.process)
                    InfoRow("Área / faena", d.area)
                    InfoRow("Turno", d.shift)
                    InfoRow("Usuario", vm.user?.name ?: "Usuario demo")
                    InfoRow("Observaciones", d.notes)
                    InfoRow("Ubicación", if (d.latitude != null && d.longitude != null) formatCoords(d.latitude, d.longitude) else "No registrada")
                }
            }
            SectionTitle("Adjuntos (${d.attachments.size})")
            d.attachments.forEach {
                Text("• ${it.name} · ${"%.1f".format(it.sizeMb)} MB", style = MaterialTheme.typography.bodyLarge)
            }
            d.extracted?.let { fields ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Datos extraídos")
                    SimulationBadge()
                }
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.padding(14.dp)) { fields.forEach { InfoRow(it.label, it.value) } }
                }
            }
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Text(
                    if (vm.isOnline) "Se guardará en el teléfono y se enviará de inmediato."
                    else "Sin conexión: se guardará en el teléfono y quedará en cola hasta recuperar la red.",
                    modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton("Guardar y enviar", onClick = { onSaved(vm.saveDraft(send = true)) })
            SecondaryButton("Guardar como borrador", { onSaved(vm.saveDraft(send = false)) }, Modifier.fillMaxWidth())
        }
    }
}
