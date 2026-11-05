package com.vitairos.industrialai.data.local

import androidx.room.withTransaction
import com.vitairos.industrialai.model.Attachment
import com.vitairos.industrialai.model.AttachmentKind
import com.vitairos.industrialai.model.Report
import com.vitairos.industrialai.model.ReportStatus
import com.vitairos.industrialai.model.TimelineEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Única puerta de acceso a Room; convierte entre entidades y los modelos de la UI. */
class ReportRepository(private val db: AppDatabase) {
    private val dao = db.reportDao()

    fun observeAll(): Flow<List<Report>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun seedIfEmpty(seed: List<Report>) {
        if (dao.count() == 0) seed.forEach { save(it) }
    }

    suspend fun resetSending() = dao.resetSending()

    suspend fun save(report: Report) = db.withTransaction {
        dao.upsertReport(report.toEntity())
        dao.clearAttachments(report.id)
        dao.clearTimeline(report.id)
        dao.insertAttachments(report.attachments.map {
            AttachmentEntity(reportId = report.id, attachmentId = it.id, name = it.name, sizeMb = it.sizeMb, kind = it.kind.name, path = it.path)
        })
        dao.insertTimeline(report.timeline.map { TimelineEntity(reportId = report.id, time = it.time, text = it.text) })
    }

    suspend fun delete(id: String) = dao.deleteReport(id)

    companion object {
        fun numberOf(id: String): Long = id.substringAfterLast('-').toLongOrNull() ?: 0L
    }

    private fun Report.toEntity() = ReportEntity(
        id = id, sortKey = numberOf(id), type = type, process = process, area = area, shift = shift,
        notes = notes, user = user, createdAt = createdAt, status = status.name, attempts = attempts,
        supervisorNote = supervisorNote, latitude = latitude, longitude = longitude,
    )

    private fun ReportWithDetails.toModel() = Report(
        id = report.id, type = report.type, process = report.process, area = report.area, shift = report.shift,
        notes = report.notes, user = report.user, createdAt = report.createdAt,
        status = ReportStatus.valueOf(report.status), attempts = report.attempts,
        attachments = attachments.sortedBy { it.rowId }.map {
            Attachment(it.attachmentId, it.name, it.sizeMb, AttachmentKind.valueOf(it.kind), it.path)
        },
        timeline = timeline.sortedBy { it.rowId }.map { TimelineEvent(it.time, it.text) },
        supervisorNote = report.supervisorNote, latitude = report.latitude, longitude = report.longitude,
    )
}
