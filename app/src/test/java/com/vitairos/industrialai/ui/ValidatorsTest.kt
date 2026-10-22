package com.vitairos.industrialai.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {
    @Test fun email() {
        assertNull(Validators.email("operador@vitairos.demo"))
        assertNotNull(Validators.email(""))
        assertNotNull(Validators.email("sin-arroba"))
        assertNotNull(Validators.email("a@b"))
    }

    @Test fun password() {
        assertNull(Validators.password("demo1234"))
        assertNotNull(Validators.password(""))
        assertNotNull(Validators.password("12345"))
    }

    @Test fun incidentNotesAreRequired() {
        assertNotNull(Validators.notes("", "Reporte de incidente v1"))
        assertNull(Validators.notes("", "Reporte de turno v1"))
        assertNull(Validators.notes("Derrame menor", "Reporte de incidente v1"))
        assertNotNull(Validators.notes("x".repeat(Validators.NOTES_MAX + 1), "Reporte de turno v1"))
    }

    @Test fun extractedFields() {
        assertNull(Validators.extractedField("Fecha del documento", "22/10/2026"))
        assertNotNull(Validators.extractedField("Fecha del documento", "31/02/2026"))
        assertNotNull(Validators.extractedField("Fecha del documento", "ayer"))
        assertNull(Validators.extractedField("Horas de operación", "11,5"))
        assertNotNull(Validators.extractedField("Horas de operación", "mucho"))
        assertNotNull(Validators.extractedField("Equipo", "  "))
    }

    @Test fun reportFormRequiresAllSelections() {
        assertTrue(ReportFormErrors.validate(Draft()).hasErrors)
        val ok = Draft(type = "Reporte de turno v1", process = "Chancado", area = "Planta", shift = "Turno A (08–20)")
        assertFalse(ReportFormErrors.validate(ok).hasErrors)
        val incident = ok.copy(type = "Reporte de incidente v1")
        assertEquals("Describa el incidente", ReportFormErrors.validate(incident).notes)
    }
}
