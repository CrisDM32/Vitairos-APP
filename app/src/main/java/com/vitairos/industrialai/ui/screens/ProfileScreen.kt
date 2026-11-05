package com.vitairos.industrialai.ui.screens

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vitairos.industrialai.ui.ReportsViewModel
import com.vitairos.industrialai.ui.components.InfoRow
import com.vitairos.industrialai.ui.components.SecondaryButton
import com.vitairos.industrialai.ui.components.SectionTitle
import com.vitairos.industrialai.ui.components.SimulationBadge
import com.vitairos.industrialai.ui.components.VitairosTopBar

@Composable
fun ProfileScreen(vm: ReportsViewModel, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        VitairosTopBar("Perfil y ajustes")
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(Modifier.padding(14.dp)) {
                    InfoRow("Usuario", vm.user?.name ?: "—")
                    InfoRow("Correo", vm.user?.email ?: "—")
                    InfoRow("Perfil", vm.user?.role?.label ?: "—")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Demostración de conectividad")
                SimulationBadge()
            }
            ToggleRow("Modo sin conexión", "Los reportes quedan en cola local", vm.demoOffline) { vm.changeDemoOffline(it) }
            ToggleRow("Simular error del servidor", "El próximo envío terminará en Error", vm.simulateServerError) { vm.setSimulateError(it) }

            SectionTitle("Acerca de")
            Text("VITAIROS Reportes · maqueta v0.1 (DSY1105)", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Datos ficticios. La API, el procesamiento de IA y los estados posteriores son simulados. Android 8.0+ · solo teléfono.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(Modifier.padding(16.dp)) {
            SecondaryButton("Cerrar sesión", onLogout, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}
