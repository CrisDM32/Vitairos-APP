package com.vitairos.industrialai.ui.screens

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vitairos.industrialai.data.MockData
import com.vitairos.industrialai.model.AttachmentKind
import com.vitairos.industrialai.ui.ReportsViewModel
import com.vitairos.industrialai.ui.components.AttachmentPreview
import com.vitairos.industrialai.ui.components.PrimaryButton
import com.vitairos.industrialai.ui.components.SecondaryButton
import com.vitairos.industrialai.ui.components.SectionTitle
import com.vitairos.industrialai.ui.components.VitairosTopBar
import com.vitairos.industrialai.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val MB = 1024.0 * 1024.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentsScreen(vm: ReportsViewModel, onBack: () -> Unit, onExtract: () -> Unit, onContinue: () -> Unit) {
    val d = vm.draft
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var previewVersion by remember { mutableIntStateOf(0) }
    var showSheet by remember { mutableStateOf(false) }
    var replaceId by remember { mutableStateOf<String?>(null) }
    var pendingPhoto by remember { mutableStateOf<File?>(null) }
    val selected = d.attachments.firstOrNull { it.id == selectedId } ?: d.attachments.lastOrNull()

    /** El archivo ya está en almacenamiento privado: se optimiza, se valida y se agrega al borrador. */
    fun accept(file: File, replace: String?) {
        scope.launch {
            withContext(Dispatchers.IO) {
                if (file.extension.lowercase() in setOf("jpg", "jpeg", "png")) ImageUtils.optimizeImage(file)
            }
            val index = d.attachments.indexOfFirst { it.id == replace }
            val err = vm.addAttachment(file.name, file.length() / MB, replace, file.path)
            if (err != null) {
                ImageUtils.deleteQuietly(file.path)
                error = err
            } else {
                error = null
                val list = vm.draft.attachments
                selectedId = (if (replace != null && index >= 0) list.getOrNull(index) else list.lastOrNull())?.id
            }
        }
    }

    fun acceptUri(uri: android.net.Uri?, replace: String?) {
        if (uri == null) return
        scope.launch {
            val (rawName, size) = withContext(Dispatchers.IO) { ImageUtils.queryMeta(context, uri) }
            val name = if (rawName.substringAfterLast('.', "").isBlank()) {
                rawName + when (context.contentResolver.getType(uri)) {
                    "image/jpeg" -> ".jpg"
                    "image/png" -> ".png"
                    "application/pdf" -> ".pdf"
                    else -> ""
                }
            } else rawName
            val pre = vm.validateAttachment(name, if (size >= 0) size / MB else 0.1, replace)
            if (pre != null) { error = pre; return@launch }
            val file = withContext(Dispatchers.IO) { ImageUtils.copyToPrivate(context, uri, name) }
            if (file == null) error = "No se pudo leer el archivo seleccionado." else accept(file, replace)
        }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val file = pendingPhoto
        pendingPhoto = null
        if (ok && file != null) accept(file, replaceId) else file?.delete()
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { acceptUri(it, replaceId) }
    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { acceptUri(it, replaceId) }

    fun launchCamera() {
        val file = ImageUtils.newPhotoFile(context)
        pendingPhoto = file
        try {
            takePicture.launch(ImageUtils.uriFor(context, file))
        } catch (e: ActivityNotFoundException) {
            pendingPhoto = null
            file.delete()
            error = "No hay una aplicación de cámara disponible en este dispositivo."
        }
    }

    Column(Modifier.fillMaxSize()) {
        VitairosTopBar("Adjuntar documento", onBack = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StepHeader(2, "Captura y vista previa")

            PrimaryButton(
                "Agregar documento", onClick = { replaceId = null; showSheet = true }, icon = Icons.Filled.Add,
                enabled = d.attachments.size < MockData.MAX_ATTACHMENTS,
            )

            AnimatedVisibility(error != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Text("  ${error.orEmpty()}", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("Probar: archivo .docx", { error = vm.validateAttachment("informe.docx", 1.0) }, Modifier.weight(1f))
                SecondaryButton("Probar: 14 MB", { error = vm.validateAttachment("escaneo_grande.jpg", 14.0) }, Modifier.weight(1f))
            }

            SectionTitle("Vista previa")
            if (selected == null) {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    Text("Aún no hay adjuntos. Tome una foto o seleccione un archivo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                AnimatedContent(
                    targetState = selected,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    modifier = Modifier.fillMaxWidth().height(290.dp),
                    contentAlignment = Alignment.Center,
                    label = "preview",
                ) { att ->
                    AttachmentPreview(att, Modifier.fillMaxWidth(0.7f).height(280.dp), version = previewVersion)
                }
                Text(
                    "Verifique que sea legible, corresponda al documento y esté bien orientado.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val canRotate = selected.kind == AttachmentKind.IMAGEN && selected.path.isNotBlank()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton("Rotar", {
                        scope.launch {
                            withContext(Dispatchers.IO) { ImageUtils.rotateImage(File(selected.path), 90f) }
                            previewVersion++
                        }
                    }, Modifier.weight(1f), Icons.Filled.Refresh, enabled = canRotate)
                    SecondaryButton("Reemplazar", { replaceId = selected.id; showSheet = true }, Modifier.weight(1f))
                    SecondaryButton("Quitar", { vm.removeAttachment(selected.id); selectedId = null }, Modifier.weight(1f), Icons.Filled.Delete)
                }
            }

            SectionTitle("Adjuntos (${d.attachments.size} de ${MockData.MAX_ATTACHMENTS})")
            Column(Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                d.attachments.forEach { a ->
                    val isSel = a.id == selected?.id
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedId = a.id },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(if (isSel) 2.dp else 1.dp, if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(a.name, style = MaterialTheme.typography.titleMedium)
                            Text("${if (a.kind == AttachmentKind.PDF) "PDF" else "Imagen"} · ${"%.1f".format(a.sizeMb)} MB · validado", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Text(
                "Formatos: JPG, PNG, PDF · máx. 10 MB por archivo · máx. ${MockData.MAX_ATTACHMENTS} archivos. Las imágenes se optimizan al guardar y se almacenan solo en la app.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton("Extraer datos con IA (Simulación)", onExtract, Modifier.fillMaxWidth(), enabled = d.attachments.isNotEmpty())
            PrimaryButton("Continuar: revisar", onContinue, enabled = d.attachments.isNotEmpty())
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }, sheetState = rememberModalBottomSheetState()) {
            Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
                Text("Origen del documento", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                SourceRow("Tomar fotografía", "Usa la cámara del teléfono", Icons.Filled.AddCircle) { showSheet = false; launchCamera() }
                SourceRow("Elegir imagen", "JPG o PNG de la galería", Icons.Filled.Search) {
                    showSheet = false
                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                SourceRow("Elegir PDF", "Documento PDF del teléfono", Icons.Filled.Info) {
                    showSheet = false
                    pickPdf.launch(arrayOf("application/pdf"))
                }
            }
        }
    }
}

@Composable
private fun SourceRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick).heightIn(min = 64.dp),
    )
}
