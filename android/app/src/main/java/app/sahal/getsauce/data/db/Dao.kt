package app.sahal.getsauce.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_jobs ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<DownloadJob>>

    @Query("SELECT * FROM download_jobs WHERE status IN ('QUEUED','RUNNING','MERGING') ORDER BY id ASC")
    suspend fun getActive(): List<DownloadJob>

    @Query("SELECT * FROM download_jobs WHERE status = 'QUEUED' ORDER BY id ASC LIMIT 1")
    suspend fun nextQueued(): DownloadJob?

    @Query("SELECT * FROM download_jobs WHERE id = :id")
    suspend fun getById(id: Long): DownloadJob?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(job: DownloadJob): Long

    @Update
    suspend fun update(job: DownloadJob)

    @Query("UPDATE download_jobs SET status = :status, updatedAt = :ts, errorMessage = :err WHERE id = :id")
    suspend fun setStatus(id: Long, status: JobStatus, ts: Long, err: String? = null)

    @Query("UPDATE download_jobs SET bytesDone = :bytes, updatedAt = :ts WHERE id = :id")
    suspend fun setProgress(id: Long, bytes: Long, ts: Long)

    @Query("DELETE FROM download_jobs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM download_jobs WHERE status IN ('COMPLETED','FAILED','CANCELLED')")
    suspend fun clearFinished()
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM download_history ORDER BY completedAt DESC")
    fun observeAll(): Flow<List<HistoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntry): Long

    @Query("DELETE FROM download_history")
    suspend fun clear()
}
