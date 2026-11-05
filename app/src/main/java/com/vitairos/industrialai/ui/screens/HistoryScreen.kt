package com.vitairos.industrialai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vitairos.industrialai.model.ReportStatus
import com.vitairos.industrialai.ui.ReportsViewModel
import com.vitairos.industrialai.ui.components.ReportCard
import com.vitairos.industrialai.ui.components.VitairosTopBar

@Composable
fun HistoryScreen(vm: ReportsViewModel, onOpenReport: (String) -> Unit) {
    var filter by remember { mutableStateOf<ReportStatus?>(null) }
    val list = vm.reports.filter { filter == null || it.status == filter }
    Column(Modifier.fillMaxSize()) {
        VitairosTopBar("Historial")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("Todos (${vm.reports.size})") }) }
            items(ReportStatus.entries) { s ->
                FilterChip(
                    selected = filter == s, onClick = { filter = s },
                    label = { Text(s.label) },
                    leadingIcon = { Icon(s.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }
        }
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (list.isEmpty()) item { Text("No hay reportes con este estado.", modifier = Modifier.padding(top = 8.dp)) }
            items(list, key = { it.id }) { ReportCard(it, onClick = { onOpenReport(it.id) }, modifier = Modifier.animateItem()) }
        }
    }
}
