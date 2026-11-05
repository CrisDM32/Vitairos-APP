package com.vitairos.industrialai.model

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ReportStatusSpec : StringSpec({

    // estado -> (deletable, inQueue)
    val expected = mapOf(
        ReportStatus.BORRADOR to (true to true),
        ReportStatus.EN_COLA to (true to true),
        ReportStatus.ENVIANDO to (false to true),
        ReportStatus.SINCRONIZADO to (false to false),
        ReportStatus.ERROR to (true to true),
        ReportStatus.REQUIERE_CORRECCION to (false to true),
    )

    "la tabla cubre todos los estados" {
        expected.keys shouldBe ReportStatus.entries.toSet()
    }

    "deletable e inQueue según el estado" {
        expected.forEach { (status, flags) ->
            withClue(status.name) {
                status.deletable shouldBe flags.first
                status.inQueue shouldBe flags.second
            }
        }
    }
})
