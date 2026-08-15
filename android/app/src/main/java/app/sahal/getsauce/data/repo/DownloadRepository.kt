package app.sahal.getsauce.data.repo

import app.sahal.getsauce.data.db.DownloadDao
import app.sahal.getsauce.data.db.DownloadJob
import app.sahal.getsauce.data.db.HistoryDao
import app.sahal.getsauce.data.db.HistoryEntry
import app.sahal.getsauce.data.db.JobStatus
import kotlinx.coroutines.flow.Flow

/**
 * Thin repository. Room DAOs already suspend, so mostly this is just a
 * convenient injection point for the service and the view models.
 */
class DownloadRepository(
    private val jobs: DownloadDao,
    private val history: HistoryDao,
) {
    fun observeJobs(): Flow<List<DownloadJob>> = jobs.observeAll()
    fun observeHistory(): Flow<List<HistoryEntry>> = history.observeAll()

    suspend fun enqueue(job: DownloadJob): Long = jobs.insert(job)
    suspend fun activeJobs(): List<DownloadJob> = jobs.getActive()
    suspend fun nextQueued(): DownloadJob? = jobs.nextQueued()
    suspend fun byId(id: Long): DownloadJob? = jobs.getById(id)

    suspend fun setStatus(id: Long, status: JobStatus, err: String? = null) =
        jobs.setStatus(id, status, System.currentTimeMillis(), err)

    suspend fun setProgress(id: Long, bytes: Long) =
        jobs.setProgress(id, bytes, System.currentTimeMillis())

    suspend fun delete(id: Long) = jobs.deleteById(id)
    suspend fun clearFinished() = jobs.clearFinished()

    suspend fun recordCompletion(entry: HistoryEntry): Long = history.insert(entry)
    suspend fun clearHistory() = history.clear()
}
