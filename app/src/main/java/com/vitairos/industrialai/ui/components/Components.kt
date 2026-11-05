package com.vitairos.industrialai.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.produceState
import com.vitairos.industrialai.model.Attachment
import com.vitairos.industrialai.model.AttachmentKind
import com.vitairos.industrialai.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vitairos.industrialai.model.Report
import com.vitairos.industrialai.model.ReportStatus
import com.vitairos.industrialai.ui.theme.Acero
import com.vitairos.industrialai.ui.theme.ChipBorrador
import com.vitairos.industrialai.ui.theme.ChipCorreccion
import com.vitairos.industrialai.ui.theme.ChipEnCola
import com.vitairos.industrialai.ui.theme.ChipEnviando
import com.vitairos.industrialai.ui.theme.ChipError
import com.vitairos.industrialai.ui.theme.ChipSincronizado
import com.vitairos.industrialai.ui.theme.Cobre
import com.vitairos.industrialai.ui.theme.Hueso
import com.vitairos.industrialai.ui.theme.Niebla
import com.vitairos.industrialai.ui.theme.Oro
import com.vitairos.industrialai.ui.theme.VerdeMineral
import com.vitairos.industrialai.ui.theme.VerdeNoche

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitairosTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = VerdeNoche,
            titleContentColor = Hueso,
            navigationIconContentColor = Hueso,
            actionIconContentColor = Hueso,
        ),
    )
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(100), label = "pressScale")
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = source,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Cobre, contentColor = VerdeNoche),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun StatusChip(status: ReportStatus, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = status,
        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
        modifier = modifier,
        label = "statusChip",
    ) { st ->
        val bg = when (st) {
            ReportStatus.BORRADOR -> ChipBorrador
            ReportStatus.EN_COLA -> ChipEnCola
            ReportStatus.ENVIANDO -> ChipEnviando
            ReportStatus.SINCRONIZADO -> ChipSincronizado
            ReportStatus.ERROR -> ChipError
            ReportStatus.REQUIERE_CORRECCION -> ChipCorreccion
        }
        Row(
            modifier = Modifier
                .background(bg, RoundedCornerShape(50))
                .border(1.dp, VerdeMineral.copy(alpha = 0.35f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusIcon(st)
            Spacer(Modifier.size(5.dp))
            Text(st.label, color = VerdeNoche, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** El icono de "Enviando" gira mientras dura el envío. */
@Composable
private fun StatusIcon(status: ReportStatus) {
    var angle = 0f
    if (status == ReportStatus.ENVIANDO) {
        val spin by rememberInfiniteTransition(label = "spin").animateFloat(
            0f, 360f, infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), label = "angle",
        )
        angle = spin
    }
    Icon(status.icon, contentDescription = null, tint = VerdeNoche, modifier = Modifier.size(16.dp).rotate(angle))
}

@Composable
fun SimulationBadge(modifier: Modifier = Modifier, text: String = "Simulación") {
    Row(
        modifier = modifier
            .background(Oro, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = VerdeNoche, modifier = Modifier.size(14.dp))
        Spacer(Modifier.size(4.dp))
        Text(text.uppercase(), color = VerdeNoche, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun ConnectionBanner(isOnline: Boolean, pending: Int, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(if (isOnline) VerdeMineral else Cobre, tween(400), label = "bannerBg")
    val fg = if (isOnline) Hueso else VerdeNoche
    val text = if (isOnline) "En línea" + if (pending > 0) " · $pending pendiente(s)" else " · todo sincronizado"
    else "Sin conexión · los reportes se guardan en el teléfono" + if (pending > 0) " ($pending en cola)" else ""
    Row(
        modifier = modifier.fillMaxWidth().background(bg).padding(horizontal = 16.dp, vertical = 8.dp).animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).background(if (isOnline) Color(0xFF7FD99A) else VerdeNoche, RoundedCornerShape(50)))
        Spacer(Modifier.size(10.dp))
        Crossfade(text, label = "bannerText") { Text(it, color = fg, style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, modifier = modifier.padding(top = 8.dp, bottom = 4.dp))
}

@Composable
fun ReportCard(report: Report, onClick: () -> Unit, modifier: Modifier = Modifier, actions: @Composable () -> Unit = {}) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(report.id, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                StatusChip(report.status)
            }
            Text(report.type, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text("${report.process} · ${report.area}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${report.createdAt} · ${report.attachments.size} adjunto(s)" + if (report.attempts > 0) " · ${report.attempts} intento(s)" else "",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f),
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            actions()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(label: String, value: String, options: List<String>, onSelect: (String) -> Unit, modifier: Modifier = Modifier, error: String? = null) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            isError = error != null,
            supportingText = { AnimatedVisibility(error != null) { Text(error.orEmpty()) } },
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach {
                DropdownMenuItem(
                    text = { Text(it, style = MaterialTheme.typography.bodyLarge) },
                    onClick = { onSelect(it); expanded = false },
                    modifier = Modifier.heightIn(min = 52.dp),
                )
            }
        }
    }
}

/** Documento ficticio dibujado: representa la vista previa de una captura. */
@Composable
fun DocumentPreview(modifier: Modifier = Modifier, isPdf: Boolean = false) {
    Box(modifier = modifier.background(Niebla, RoundedCornerShape(12.dp)).padding(18.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(Color.White, size = size, cornerRadius = CornerRadius(8f, 8f))
            val w = size.width
            drawRect(VerdeNoche, topLeft = Offset(w * 0.08f, size.height * 0.05f), size = Size(w * 0.5f, size.height * 0.035f))
            drawRect(Cobre, topLeft = Offset(w * 0.70f, size.height * 0.05f), size = Size(w * 0.22f, size.height * 0.035f))
            var y = size.height * 0.14f
            repeat(9) { i ->
                drawRect(Acero, topLeft = Offset(w * 0.08f, y), size = Size(w * (if (i % 3 == 2) 0.55f else 0.84f), size.height * 0.018f))
                y += size.height * 0.045f
            }
            drawRect(Acero, topLeft = Offset(w * 0.08f, y + size.height * 0.02f), size = Size(w * 0.84f, size.height * 0.22f), style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
        }
        if (isPdf) {
            Text("PDF", color = VerdeNoche, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.TopEnd).background(Oro, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun Spacer16() = Spacer(Modifier.height(16.dp))

/** Entrada escalonada: aparece con fundido y desplazamiento, con retraso según [index]. */
@Composable
fun Appear(index: Int = 0, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(tween(350, delayMillis = index * 70)) + slideInVertically(tween(350, delayMillis = index * 70)) { it / 6 },
    ) { content() }
}

/** Vista previa real del adjunto (imagen o primera página del PDF). Sin archivo, muestra un documento de ejemplo. */
@Composable
fun AttachmentPreview(attachment: Attachment, modifier: Modifier = Modifier, version: Int = 0) {
    val bitmap by produceState<Bitmap?>(null, attachment.path, version) {
        value = if (attachment.path.isBlank()) null
        else withContext(Dispatchers.IO) { ImageUtils.loadPreview(attachment.path, attachment.kind) }
    }
    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Vista previa de ${attachment.name}",
            contentScale = ContentScale.Fit,
            modifier = modifier.clip(RoundedCornerShape(12.dp)).background(Color.White),
        )
    } else {
        DocumentPreview(modifier, isPdf = attachment.kind == AttachmentKind.PDF)
    }
}
