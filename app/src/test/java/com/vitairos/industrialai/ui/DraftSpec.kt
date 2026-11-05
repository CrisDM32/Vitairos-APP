package com.vitairos.industrialai.ui

import com.vitairos.industrialai.model.Attachment
import com.vitairos.industrialai.model.AttachmentKind
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class DraftSpec : StringSpec({

    val complete = Draft(type = "Reporte de turno v1", process = "Chancado", area = "Planta", shift = "Turno A (08–20)")

    "metadataComplete exige tipo, proceso, área y turno" {
        complete.metadataComplete.shouldBeTrue()
        complete.copy(type = "").metadataComplete.shouldBeFalse()
        complete.copy(process = " ").metadataComplete.shouldBeFalse()
        complete.copy(area = "").metadataComplete.shouldBeFalse()
        complete.copy(shift = "").metadataComplete.shouldBeFalse()
    }

    "hasLocation requiere ambas coordenadas" {
        Draft().hasLocation.shouldBeFalse()
        Draft(latitude = -33.4).hasLocation.shouldBeFalse()
        Draft(longitude = -70.6).hasLocation.shouldBeFalse()
        Draft(latitude = -33.4, longitude = -70.6).hasLocation.shouldBeTrue()
    }

    "un borrador nuevo está vacío" {
        Draft().isBlank.shouldBeTrue()
    }

    "isBlank es false si hay cualquier dato" {
        Draft(editingId = "RPT-2026-0001").isBlank.shouldBeFalse()
        Draft(type = "Reporte de turno v1").isBlank.shouldBeFalse()
        Draft(notes = "algo").isBlank.shouldBeFalse()
        Draft(attachments = listOf(Attachment("N1", "a.jpg", 1.0, AttachmentKind.IMAGEN))).isBlank.shouldBeFalse()
        Draft(latitude = 1.0, longitude = 2.0).isBlank.shouldBeFalse()
    }
})
