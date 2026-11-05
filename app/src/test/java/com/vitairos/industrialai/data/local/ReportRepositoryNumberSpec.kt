package com.vitairos.industrialai.data.local

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ReportRepositoryNumberSpec : StringSpec({

    "extrae el número correlativo del id" {
        ReportRepository.numberOf("RPT-2026-0012") shouldBe 12L
        ReportRepository.numberOf("RPT-2026-1000") shouldBe 1000L
    }

    "devuelve 0 con ids mal formados" {
        ReportRepository.numberOf("") shouldBe 0L
        ReportRepository.numberOf("RPT-2026-") shouldBe 0L
        ReportRepository.numberOf("RPT-2026-abc") shouldBe 0L
    }
})
