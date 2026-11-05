package com.vitairos.industrialai.ui

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.localDate
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ValidatorsKotestSpec : StringSpec({

    "email" {
        Validators.email("operador@vitairos.demo").shouldBeNull()
        Validators.email("").shouldNotBeNull()
        Validators.email("sin-arroba").shouldNotBeNull()
        Validators.email("a@b").shouldNotBeNull()
    }

    "email acepta espacios alrededor y rechaza dominio sin TLD" {
        Validators.email("  operador@vitairos.demo  ").shouldBeNull()
        Validators.email("operador@empresa").shouldNotBeNull()
        Validators.email("   ") shouldBe "Ingrese su correo"
    }

    "password" {
        Validators.password("demo1234").shouldBeNull()
        Validators.password("").shouldNotBeNull()
        Validators.password("12345").shouldNotBeNull()
    }

    "password con exactamente PASSWORD_MIN caracteres es válida" {
        Validators.password("x".repeat(Validators.PASSWORD_MIN)).shouldBeNull()
        Validators.password("x".repeat(Validators.PASSWORD_MIN - 1)) shouldBe "Mínimo ${Validators.PASSWORD_MIN} caracteres"
    }

    "required" {
        Validators.required("", "Obligatorio") shouldBe "Obligatorio"
        Validators.required("   ", "Obligatorio") shouldBe "Obligatorio"
        Validators.required("Planta", "Obligatorio").shouldBeNull()
    }

    "las notas son obligatorias en reportes de incidente" {
        Validators.notes("", "Reporte de incidente v1").shouldNotBeNull()
        Validators.notes("", "Reporte de turno v1").shouldBeNull()
        Validators.notes("Derrame menor", "Reporte de incidente v1").shouldBeNull()
        Validators.notes("x".repeat(Validators.NOTES_MAX + 1), "Reporte de turno v1").shouldNotBeNull()
    }

    "notas: límite exacto y detección de incidente sin distinguir mayúsculas" {
        Validators.notes("x".repeat(Validators.NOTES_MAX), "Reporte de turno v1").shouldBeNull()
        Validators.notes("", "REPORTE DE INCIDENTE") shouldBe "Describa el incidente"
    }

    "fechas: año bisiesto" {
        Validators.date("29/02/2028").shouldBeNull()
        Validators.date("29/02/2027").shouldNotBeNull()
        Validators.date(" 01/01/2026 ").shouldBeNull()
        Validators.date("2026-01-01").shouldNotBeNull()
    }

    "números con coma o punto decimal" {
        Validators.number("11").shouldBeNull()
        Validators.number("11,5").shouldBeNull()
        Validators.number("11.5").shouldBeNull()
        Validators.number("1,2,3").shouldNotBeNull()
        Validators.number("-3").shouldNotBeNull()
    }

    "campos extraídos" {
        Validators.extractedField("Fecha del documento", "22/10/2026").shouldBeNull()
        Validators.extractedField("Fecha del documento", "31/02/2026").shouldNotBeNull()
        Validators.extractedField("Fecha del documento", "ayer").shouldNotBeNull()
        Validators.extractedField("Horas de operación", "11,5").shouldBeNull()
        Validators.extractedField("Horas de operación", "mucho").shouldNotBeNull()
        Validators.extractedField("Equipo", "  ").shouldNotBeNull()
    }

    "el formulario de reporte requiere todas las selecciones" {
        ReportFormErrors.validate(Draft()).hasErrors.shouldBeTrue()

        val ok = Draft(
            type = "Reporte de turno v1",
            process = "Chancado",
            area = "Planta",
            shift = "Turno A (08–20)",
        )
        ReportFormErrors.validate(ok).hasErrors.shouldBeFalse()

        val incident = ok.copy(type = "Reporte de incidente v1")
        ReportFormErrors.validate(incident).notes shouldBe "Describa el incidente"
    }

    "el formulario devuelve el mensaje concreto de cada campo vacío" {
        val errors = ReportFormErrors.validate(Draft())
        errors.type shouldBe "Seleccione el tipo de reporte"
        errors.process shouldBe "Seleccione el proceso asociado"
        errors.area shouldBe "Seleccione el área o faena"
        errors.shift shouldBe "Seleccione el turno"
        errors.notes.shouldBeNull()
    }

    // ---- Pruebas basadas en propiedades ----

    "propiedad: cualquier nota de más de NOTES_MAX caracteres es rechazada" {
        checkAll(Arb.string(Validators.NOTES_MAX + 1, Validators.NOTES_MAX + 200)) { text ->
            Validators.notes(text, "Reporte de turno v1") shouldBe "Máximo ${Validators.NOTES_MAX} caracteres"
        }
    }

    "propiedad: cualquier fecha real en formato dd/MM/yyyy es aceptada" {
        val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        checkAll(Arb.localDate(LocalDate.of(1900, 1, 1), LocalDate.of(2100, 12, 31))) { date ->
            Validators.date(date.format(fmt)).shouldBeNull()
        }
    }
})
