package com.vitairos.industrialai.data

import com.vitairos.industrialai.model.*

/** Datos ficticios de la maqueta. Se reemplazarán por Room/DataStore y una API en fases posteriores. */
object MockData {
    val users = listOf(
        DemoUser("operador@vitairos.demo", "Camila Rojas (demo)", Role.OPERADOR),
        DemoUser("supervisor@vitairos.demo", "Andrés Soto (demo)", Role.SUPERVISOR),
    )
    const val DEMO_PASSWORD = "demo1234"

    val reportTypes = listOf(
        "Reporte de turno v1",
        "Reporte de mantención v1",
        "Reporte de incidente v1",
        "Checklist de inspección v2",
    )

    val processes = listOf("Chancado", "Molienda", "Flotación", "Transporte de mineral", "Mantención mecánica")
    val areas = listOf("Faena Norte", "Faena Sur", "Planta", "Taller")
    val shifts = listOf("Turno A (08–20)", "Turno B (20–08)", "Día administrativo")

    const val MAX_ATTACHMENTS = 5
    const val MAX_FILE_MB = 10.0
    val retryBackoffSeconds = listOf(5, 15, 30)

    val extractedFields = listOf(
        ExtractedField("Tipo de documento", "Reporte de turno"),
        ExtractedField("Fecha del documento", "22/10/2026"),
        ExtractedField("Responsable", "C. Rojas"),
        ExtractedField("Equipo", "Chancador primario CH-01"),
        ExtractedField("Horas de operación", "11,5"),
        ExtractedField("Observación detectada", "Vibración alta en rodamiento"),
    )

    private fun att(n: Int, name: String, mb: Double, k: AttachmentKind = AttachmentKind.IMAGEN) =
        Attachment("A$n", name, mb, k)

    private fun report(
        n: Int, type: String, process: String, area: String, shift: String, time: String,
        status: ReportStatus, attempts: Int, files: List<Attachment>,
        events: List<TimelineEvent>, notes: String = "", supervisorNote: String? = null,
    ) = Report(
        id = "RPT-2026-%04d".format(n), type = type, process = process, area = area, shift = shift,
        notes = notes, user = "Camila Rojas (demo)", createdAt = time, status = status,
        attempts = attempts, attachments = files, timeline = events, supervisorNote = supervisorNote,
    )

    fun initialReports(): List<Report> = listOf(
        report(
            12, reportTypes[0], processes[0], areas[0], shifts[0], "22/10/2026 09:12",
            ReportStatus.BORRADOR, 0, listOf(att(1, "turno_chancado.jpg", 2.1)),
            listOf(TimelineEvent("22/10 09:12", "Borrador creado en el dispositivo")),
        ),
        report(
            11, reportTypes[1], processes[4], areas[3], shifts[0], "22/10/2026 08:40",
            ReportStatus.EN_COLA, 0,
            listOf(att(2, "mantencion_bomba.jpg", 3.4), att(3, "ot_4521.pdf", 0.8, AttachmentKind.PDF)),
            listOf(
                TimelineEvent("22/10 08:40", "Reporte creado"),
                TimelineEvent("22/10 08:41", "Sin conexión: guardado localmente y en cola"),
            ),
            notes = "Cambio de sello mecánico",
        ),
        report(
            10, reportTypes[0], processes[1], areas[2], shifts[1], "21/10/2026 23:55",
            ReportStatus.EN_COLA, 0, listOf(att(4, "turno_molienda.png", 1.9)),
            listOf(
                TimelineEvent("21/10 23:55", "Reporte creado"),
                TimelineEvent("21/10 23:56", "Pendiente de conexión"),
            ),
        ),
        report(
            9, reportTypes[2], processes[3], areas[1], shifts[1], "21/10/2026 22:10",
            ReportStatus.ERROR, 3, listOf(att(5, "incidente_camion.jpg", 4.2)),
            listOf(
                TimelineEvent("21/10 22:10", "Reporte creado"),
                TimelineEvent("21/10 22:11", "Intento 1 fallido (reintento en 5 s)"),
                TimelineEvent("21/10 22:12", "Intento 2 fallido (reintento en 15 s)"),
                TimelineEvent("21/10 22:12", "Intento 3 fallido (reintento en 30 s)"),
                TimelineEvent("21/10 22:13", "Reintentos automáticos agotados: reintento manual disponible"),
            ),
            notes = "Derrame menor en ruta 3",
        ),
        report(
            8, reportTypes[3], processes[2], areas[2], shifts[0], "21/10/2026 10:30",
            ReportStatus.REQUIERE_CORRECCION, 1, listOf(att(6, "checklist_flotacion.jpg", 2.8)),
            listOf(
                TimelineEvent("21/10 10:30", "Reporte creado"),
                TimelineEvent("21/10 10:32", "Sincronizado"),
                TimelineEvent("21/10 14:05", "Supervisor marcó: requiere corrección (simulado)"),
            ),
            supervisorNote = "La fecha del checklist no es legible. Adjunte una nueva fotografía.",
        ),
        report(
            7, reportTypes[0], processes[0], areas[0], shifts[1], "20/10/2026 20:15",
            ReportStatus.SINCRONIZADO, 1, listOf(att(7, "turno_20oct.pdf", 1.2, AttachmentKind.PDF)),
            listOf(
                TimelineEvent("20/10 20:15", "Reporte creado"),
                TimelineEvent("20/10 20:16", "Sincronizado correctamente"),
                TimelineEvent("20/10 20:20", "En revisión (estado simulado)"),
            ),
        ),
        report(
            6, reportTypes[1], processes[4], areas[3], shifts[0], "20/10/2026 11:02",
            ReportStatus.SINCRONIZADO, 2, listOf(att(8, "mantencion_20oct.jpg", 3.0)),
            listOf(
                TimelineEvent("20/10 11:02", "Reporte creado"),
                TimelineEvent("20/10 11:03", "Intento 1 fallido: sin conexión"),
                TimelineEvent("20/10 11:10", "Sincronizado correctamente"),
            ),
        ),
        report(
            5, reportTypes[3], processes[1], areas[2], shifts[2], "19/10/2026 09:45",
            ReportStatus.SINCRONIZADO, 1, listOf(att(9, "checklist_molienda.png", 2.2)),
            listOf(
                TimelineEvent("19/10 09:45", "Reporte creado"),
                TimelineEvent("19/10 09:46", "Sincronizado correctamente"),
                TimelineEvent("19/10 09:50", "Procesado (estado simulado)"),
            ),
        ),
    )
}
