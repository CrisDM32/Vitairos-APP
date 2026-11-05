package com.vitairos.industrialai.ui

import com.vitairos.industrialai.data.MockData
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith

class AttachmentValidatorSpec : StringSpec({

    fun validate(name: String, sizeMb: Double = 1.0, count: Int = 0, replacing: Boolean = false) =
        Validators.attachment(name, sizeMb, count, replacing)

    "formatos permitidos sin importar mayúsculas" {
        listOf("foto.jpg", "foto.JPEG", "captura.png", "orden.PDF").forEach { validate(it).shouldBeNull() }
    }

    "formato no permitido o sin extensión" {
        validate("informe.docx") shouldBe "Formato no permitido (.docx). Use JPG, PNG o PDF."
        validate("archivo").shouldStartWith("Formato no permitido")
    }

    "archivo vacío" {
        validate("foto.jpg", sizeMb = 0.0) shouldBe "El archivo está vacío."
    }

    "límite de tamaño de 10 MB" {
        validate("foto.jpg", sizeMb = MockData.MAX_FILE_MB).shouldBeNull()
        validate("foto.jpg", sizeMb = 10.01) shouldBe "El archivo supera el máximo de 10 MB."
    }

    "límite de cantidad de adjuntos, salvo al reemplazar" {
        validate("foto.jpg", count = MockData.MAX_ATTACHMENTS - 1).shouldBeNull()
        validate("foto.jpg", count = MockData.MAX_ATTACHMENTS) shouldBe "Máximo ${MockData.MAX_ATTACHMENTS} archivos por reporte."
        validate("foto.jpg", count = MockData.MAX_ATTACHMENTS, replacing = true).shouldBeNull()
    }
})
