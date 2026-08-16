package app.sahal.moegrab.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A job the user has enqueued. Populated when they tap Download on the info
 * screen; drained by DownloadService.
 */
@Entity(tableName = "download_jobs")
data class DownloadJob(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceUrl: String,
    val title: String,
    val site: String,
    /** Opaque payload token from ExtractedData.payload — Go bridge round-trips it. */
    val payload: String,
    val streamId: String,
    val streamLabel: String,
    val estimatedBytes: Long,
    /** Absolute path under app-private storage; final SAF copy handled post-download. */
    val stagingDir: String,
    /** SAF tree URI string chosen by the user in Settings, or null for cache-only. */
    val destTreeUri: String?,
    val status: JobStatus,
    /** For QUEUED/RUNNING/FAILED, the millisecond timestamp when the row was updated. */
    val updatedAt: Long,
    val bytesDone: Long = 0L,
    val errorMessage: String? = null,
)

enum class JobStatus { QUEUED, RUNNING, MERGING, COMPLETED, FAILED, CANCELLED }

@Entity(tableName = "download_history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceUrl: String,
    val title: String,
    val site: String,
    val streamLabel: String,
    val finalUri: String,   // SAF or file:// path where the finished file lives
    val sizeBytes: Long,
    val completedAt: Long,
)
