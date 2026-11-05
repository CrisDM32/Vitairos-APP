package com.vitairos.industrialai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vitairos.industrialai.data.MockData
import com.vitairos.industrialai.ui.ReportsViewModel
import androidx.compose.animation.AnimatedVisibility
import com.vitairos.industrialai.ui.Validators
import com.vitairos.industrialai.ui.components.AttachmentPreview
import com.vitairos.industrialai.ui.components.DocumentPreview
import com.vitairos.industrialai.ui.components.PrimaryButton
import com.vitairos.industrialai.ui.components.SecondaryButton
import com.vitairos.industrialai.ui.components.SimulationBadge
import com.vitairos.industrialai.ui.components.VitairosTopBar
import kotlinx.coroutines.delay

private val steps = listOf("Enviando imagen a VITAIROS Industrial AI…", "Detectando campos del documento…", "Normalizando valores…")

@Composable
fun ExtractionScreen(vm: ReportsViewModel, onBack: () -> Unit, onConfirm: () -> Unit) {
    var done by remember { mutableStateOf(vm.draft.extracted != null) }
    var step by remember { mutableStateOf(0) }
    var submitted by remember { mutableStateOf(false) }
    val fields = remember { mutableStateListOf<String>().apply { addAll((vm.draft.extracted ?: MockData.extractedFields).map { it.value }) } }

    LaunchedEffect(done) {
        if (!done) {
            steps.indices.forEach { step = it; delay(900) }
            done = true
        }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        VitairosTopBar("Extracción de datos", onBack = onBack)
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SimulationBadge()
            Text("Resultado de ejemplo: no se usa OCR ni IA real.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!done) {
            Column(Modifier.weight(1f).fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 5.dp)
                Text("\n${steps[step]}", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text("Procesamiento simulado", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    val first = vm.draft.attachments.firstOrNull()
                    if (first != null) AttachmentPreview(first, Modifier.fillMaxWidth(0.5f).height(170.dp))
                    else DocumentPreview(Modifier.fillMaxWidth(0.5f).height(170.dp))
                }
                Text("Datos estructurados detectados. Revise, corrija y confirme.", style = MaterialTheme.typography.titleMedium)
                MockData.extractedFields.forEachIndexed { i, f ->
                    val err = if (submitted) Validators.extractedField(f.label, fields[i]) else null
                    OutlinedTextField(
                        value = fields[i], onValueChange = { fields[i] = it },
                        label = { Text(f.label) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        isError = err != null,
                        supportingText = { AnimatedVisibility(err != null) { Text(err.orEmpty()) } },
                    )
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("Confirmar datos", onClick = {
                    submitted = true
                    val valid = MockData.extractedFields.indices.all { Validators.extractedField(MockData.extractedFields[it].label, fields[it]) == null }
                    if (valid) {
                        vm.updateDraft { d -> d.copy(extracted = MockData.extractedFields.mapIndexed { i, f -> f.copy(value = fields[i].trim()) }) }
                        onConfirm()
                    }
                })
                SecondaryButton("Omitir extracción", onBack, Modifier.fillMaxWidth())
            }
        }
    }
}
