package com.vitairos.industrialai.ui

import com.vitairos.industrialai.data.MockData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/** Validaciones puras de formularios. Devuelven el mensaje de error o null si el valor es válido. */
object Validators {
    const val NOTES_MAX = 300
    const val PASSWORD_MIN = 6

    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val numberRegex = Regex("^\\d+([.,]\\d+)?$")
    private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)

    fun email(value: String): String? = when {
        value.isBlank() -> "Ingrese su correo"
        !emailRegex.matches(value.trim()) -> "Correo no válido (ej.: nombre@empresa.cl)"
        else -> null
    }

    fun password(value: String): String? = when {
        value.isBlank() -> "Ingrese su contraseña"
        value.length < PASSWORD_MIN -> "Mínimo $PASSWORD_MIN caracteres"
        else -> null
    }

    fun required(value: String, message: String): String? = if (value.isBlank()) message else null

    /** Las observaciones son obligatorias para reportes de incidente y tienen un máximo de caracteres. */
    fun notes(value: String, reportType: String): String? = when {
        value.length > NOTES_MAX -> "Máximo $NOTES_MAX caracteres"
        reportType.contains("incidente", ignoreCase = true) && value.isBlank() -> "Describa el incidente"
        else -> null
    }

    fun date(value: String): String? =
        runCatching { LocalDate.parse(value.trim(), dateFormat) }.fold({ null }, { "Fecha no válida (dd/mm/aaaa)" })

    fun number(value: String): String? = if (numberRegex.matches(value.trim())) null else "Ingrese un número (ej.: 11,5)"

    /**
     * Valida formato, tamaño y cantidad de un adjunto. [currentCount] es la cantidad ya adjunta;
     * si [replacing] es true no se aplica el límite de cantidad.
     */
    fun attachment(name: String, sizeMb: Double, currentCount: Int, replacing: Boolean): String? {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when {
            ext !in setOf("jpg", "jpeg", "png", "pdf") -> "Formato no permitido (.$ext). Use JPG, PNG o PDF."
            sizeMb <= 0.0 -> "El archivo está vacío."
            sizeMb > MockData.MAX_FILE_MB -> "El archivo supera el máximo de 10 MB."
            !replacing && currentCount >= MockData.MAX_ATTACHMENTS -> "Máximo ${MockData.MAX_ATTACHMENTS} archivos por reporte."
            else -> null
        }
    }

    /** Valida un campo extraído por la simulación según su etiqueta. */
    fun extractedField(label: String, value: String): String? = when {
        value.isBlank() -> "Este campo no puede estar vacío"
        label.startsWith("Fecha") -> date(value)
        label.startsWith("Horas") -> number(value)
        else -> null
    }
}

/** Errores por campo del formulario de nuevo reporte. */
data class ReportFormErrors(
    val type: String? = null,
    val process: String? = null,
    val area: String? = null,
    val shift: String? = null,
    val notes: String? = null,
) {
    val hasErrors get() = listOf(type, process, area, shift, notes).any { it != null }

    companion object {
        fun validate(d: Draft) = ReportFormErrors(
            type = Validators.required(d.type, "Seleccione el tipo de reporte"),
            process = Validators.required(d.process, "Seleccione el proceso asociado"),
            area = Validators.required(d.area, "Seleccione el área o faena"),
            shift = Validators.required(d.shift, "Seleccione el turno"),
            notes = Validators.notes(d.notes, d.type),
        )
    }
}
