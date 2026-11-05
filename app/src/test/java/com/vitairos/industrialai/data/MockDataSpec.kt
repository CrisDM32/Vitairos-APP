package com.vitairos.industrialai.data

import com.vitairos.industrialai.ui.Validators
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldNotContainDuplicates
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.string.shouldMatch

class MockDataSpec : StringSpec({

    val reports = MockData.initialReports()

    "los ids de los reportes iniciales son únicos y con formato correcto" {
        reports.map { it.id }.shouldNotContainDuplicates()
        reports.forEach { it.id shouldMatch Regex("RPT-2026-\\d{4}") }
    }

    "los campos extraídos de ejemplo son válidos" {
        MockData.extractedFields.forEach { Validators.extractedField(it.label, it.value).shouldBeNull() }
    }

    "los reportes iniciales usan valores existentes en los catálogos" {
        reports.forEach {
            it.type shouldBeIn MockData.reportTypes
            it.process shouldBeIn MockData.processes
            it.area shouldBeIn MockData.areas
            it.shift shouldBeIn MockData.shifts
        }
    }

    "los adjuntos de ejemplo respetan los límites" {
        reports.forEach { r ->
            r.attachments.size shouldBeLessThanOrEqual MockData.MAX_ATTACHMENTS
            r.attachments.forEach { it.sizeMb shouldBeLessThanOrEqual MockData.MAX_FILE_MB }
        }
    }

    "los usuarios y la contraseña demo pasan las validaciones de login" {
        Validators.password(MockData.DEMO_PASSWORD).shouldBeNull()
        MockData.users.forEach { Validators.email(it.email).shouldBeNull() }
    }
})
