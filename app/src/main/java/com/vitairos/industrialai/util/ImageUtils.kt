package com.vitairos.industrialai.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.vitairos.industrialai.model.AttachmentKind
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max

/** Archivos adjuntos: siempre en almacenamiento privado de la app (nunca en carpetas públicas). */
object ImageUtils {
    private const val MAX_SIDE = 2000
    private const val JPEG_QUALITY = 80

    fun attachmentsDir(context: Context): File = File(context.filesDir, "attachments").apply { mkdirs() }

    fun stamp(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"))

    fun newPhotoFile(context: Context): File = File(attachmentsDir(context), "IMG_${stamp()}.jpg")

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Nombre y tamaño en bytes de un documento elegido por el usuario. */
    fun queryMeta(context: Context, uri: Uri): Pair<String, Long> {
        var name = uri.lastPathSegment ?: "archivo"
        var size = -1L
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = c.getString(it) ?: name }
                c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { size = c.getLong(it) }
            }
        }
        return name to size
    }

    /** Copia un documento externo al almacenamiento privado. Devuelve null si no se pudo leer. */
    fun copyToPrivate(context: Context, uri: Uri, displayName: String): File? = runCatching {
        val safe = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val target = File(attachmentsDir(context), "${stamp()}_$safe")
        context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } }
            ?: return null
        target
    }.getOrNull()

    /** Reduce tamaño y peso de una imagen sin perder legibilidad y corrige su orientación EXIF. */
    fun optimizeImage(file: File) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            if (bounds.outWidth <= 0) return
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
            var bmp = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return
            val matrix = Matrix()
            val degrees = when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (degrees != 0f) matrix.postRotate(degrees)
            val scale = MAX_SIDE.toFloat() / max(bmp.width, bmp.height)
            if (scale < 1f) matrix.postScale(scale, scale)
            if (!matrix.isIdentity) bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
            val png = file.extension.equals("png", ignoreCase = true)
            FileOutputStream(file).use {
                bmp.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it)
            }
        }
    }

    /** Gira una imagen guardada (el archivo queda rotado de verdad). */
    fun rotateImage(file: File, degrees: Float): Boolean = runCatching {
        val bmp = BitmapFactory.decodeFile(file.path) ?: return false
        val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(degrees) }, true)
        val png = file.extension.equals("png", ignoreCase = true)
        FileOutputStream(file).use { rotated.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        true
    }.getOrDefault(false)

    /** Miniatura para la vista previa: imagen submuestreada o primera página de un PDF. */
    fun loadPreview(path: String, kind: AttachmentKind, maxSide: Int = 1000): Bitmap? = runCatching {
        val file = File(path)
        if (!file.exists()) return null
        if (kind == AttachmentKind.IMAGEN) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        } else {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { renderer ->
                    renderer.openPage(0).use { page ->
                        val w = maxSide * 3 / 4
                        val h = w * page.height / page.width
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp
                    }
                }
            }
        }
    }.getOrNull()

    fun deleteQuietly(path: String) {
        if (path.isNotBlank()) runCatching { File(path).delete() }
    }
}
