package com.vitairos.industrialai.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Transaction
    @Query("SELECT * FROM reports ORDER BY sortKey DESC")
    fun observeAll(): Flow<List<ReportWithDetails>>

    @Query("SELECT COUNT(*) FROM reports")
    suspend fun count(): Int

    @Upsert
    suspend fun upsertReport(report: ReportEntity)

    @Insert
    suspend fun insertAttachments(items: List<AttachmentEntity>)

    @Insert
    suspend fun insertTimeline(items: List<TimelineEntity>)

    @Query("DELETE FROM attachments WHERE reportId = :id")
    suspend fun clearAttachments(id: String)

    @Query("DELETE FROM timeline WHERE reportId = :id")
    suspend fun clearTimeline(id: String)

    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun deleteReport(id: String)

    /** Tras un cierre inesperado, lo que estaba enviándose vuelve a la cola. */
    @Query("UPDATE reports SET status = 'EN_COLA' WHERE status = 'ENVIANDO'")
    suspend fun resetSending()
}

@Database(
    entities = [ReportEntity::class, AttachmentEntity::class, TimelineEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao

    companion object {
        private val instances = mutableMapOf<String, AppDatabase>()
        @Synchronized fun get(context: Context, name: String = "vitairos.db"): AppDatabase =
            instances.getOrPut(name) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, name)
                    .addMigrations(object : androidx.room.migration.Migration(1, 2) {
                        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            db.execSQL("ALTER TABLE reports ADD COLUMN weather TEXT")
                            db.execSQL("ALTER TABLE reports ADD COLUMN dirty INTEGER NOT NULL DEFAULT 0")
                            db.execSQL("ALTER TABLE reports ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
                        }
                    }).build()
            }
    }
}
