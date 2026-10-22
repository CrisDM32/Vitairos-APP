package com.vitairos.industrialai.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    /** Número correlativo del id; define el orden (más reciente primero). */
    val sortKey: Long,
    val type: String,
    val process: String,
    val area: String,
    val shift: String,
    val notes: String,
    val user: String,
    val createdAt: String,
    val status: String,
    val attempts: Int,
    val supervisorNote: String?,
    val latitude: Double?,
    val longitude: Double?,
)

@Entity(
    tableName = "attachments",
    foreignKeys = [ForeignKey(ReportEntity::class, ["id"], ["reportId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("reportId")],
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val reportId: String,
    val attachmentId: String,
    val name: String,
    val sizeMb: Double,
    val kind: String,
    val path: String,
)

@Entity(
    tableName = "timeline",
    foreignKeys = [ForeignKey(ReportEntity::class, ["id"], ["reportId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("reportId")],
)
data class TimelineEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val reportId: String,
    val time: String,
    val text: String,
)

data class ReportWithDetails(
    @Embedded val report: ReportEntity,
    @Relation(parentColumn = "id", entityColumn = "reportId") val attachments: List<AttachmentEntity>,
    @Relation(parentColumn = "id", entityColumn = "reportId") val timeline: List<TimelineEntity>,
)
