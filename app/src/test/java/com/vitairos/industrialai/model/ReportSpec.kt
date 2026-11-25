package com.vitairos.industrialai.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class ReportSpec : StringSpec({

    fun report(latitude: Double?, longitude: Double?) = Report(
        id = "RPT-2026-0001",
        type = "Reporte de turno v1",
        process = "Chancado",
        area = "Planta",
        shift = "Turno A (08–20)",
        notes = "",
        user = "Operador",
        createdAt = "25/11/2026 10:00",
        status = ReportStatus.BORRADOR,
        attempts = 0,
        attachments = emptyList(),
        timeline = emptyList(),
        latitude = latitude,
        longitude = longitude,
    )

    "hasLocation requiere ambas coordenadas" {
        report(null, null).hasLocation.shouldBeFalse()
        report(-33.4, null).hasLocation.shouldBeFalse()
        report(null, -70.6).hasLocation.shouldBeFalse()
    }

    "hasLocation acepta coordenadas con valor cero" {
        report(0.0, 0.0).hasLocation.shouldBeTrue()
    }
})