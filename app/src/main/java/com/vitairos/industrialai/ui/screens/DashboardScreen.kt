package com.vitairos.industrialai.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import com.vitairos.industrialai.ui.components.Appear
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.vitairos.industrialai.R
import com.vitairos.industrialai.model.ReportStatus
import com.vitairos.industrialai.ui.ReportsViewModel
import com.vitairos.industrialai.ui.components.ConnectionBanner
import com.vitairos.industrialai.ui.components.PrimaryButton
import com.vitairos.industrialai.ui.components.ReportCard
import com.vitairos.industrialai.ui.components.SectionTitle
import com.vitairos.industrialai.ui.components.VitairosTopBar

@Composable
fun DashboardScreen(
    vm: ReportsViewModel,
    onNewReport: () -> Unit,
    onOpenReport: (String) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val reports = vm.reports
    val attention = reports.filter { it.status == ReportStatus.ERROR || it.status == ReportStatus.REQUIERE_CORRECCION }
    Column(Modifier.fillMaxSize()) {
        VitairosTopBar("Inicio", actions = {
            Image(painterResource(R.drawable.logo_vitairos), contentDescription = "VITAIROS", modifier = Modifier.size(44.dp))
        })
        ConnectionBanner(vm.isOnline, vm.pendingCount)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Appear(0) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Hola, ${vm.user?.name?.substringBefore(" ") ?: "operador"}", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "${vm.user?.role?.label ?: "Operador"} · Turno A · Faena Norte",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PrimaryButton("Nuevo reporte", onClick = onNewReport, icon = Icons.Filled.Add)
                }
            }

            SectionTitle("Resumen")
            Appear(1) { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CountTile(ReportStatus.EN_COLA, reports.count { it.status == ReportStatus.EN_COLA }, onOpenQueue)
                CountTile(ReportStatus.ERROR, reports.count { it.status == ReportStatus.ERROR }, onOpenQueue)
            } }
            Appear(2) { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CountTile(ReportStatus.BORRADOR, reports.count { it.status == ReportStatus.BORRADOR }, onOpenQueue)
                CountTile(ReportStatus.SINCRONIZADO, reports.count { it.status == ReportStatus.SINCRONIZADO }, onOpenHistory)
            } }

            if (attention.isNotEmpty()) {
                SectionTitle("Requieren atención")
                attention.forEach { ReportCard(it, onClick = { onOpenReport(it.id) }) }
            }

            SectionTitle("Últimos reportes")
            reports.take(3).forEach { ReportCard(it, onClick = { onOpenReport(it.id) }) }
        }
    }
}

@Composable
private fun RowScope.CountTile(status: ReportStatus, count: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.weight(1f).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp).fillMaxWidth()) {
            AnimatedContent(count, label = "count") { Text("$it", style = MaterialTheme.typography.headlineMedium) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(status.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(status.label, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
