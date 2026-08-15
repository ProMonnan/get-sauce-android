package app.sahal.getsauce.download

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import app.sahal.getsauce.R
import app.sahal.getsauce.app.App
import app.sahal.getsauce.bridge.BridgeProgressListener
import app.sahal.getsauce.bridge.ExtractorBridge
import app.sahal.getsauce.data.db.DownloadJob
import app.sahal.getsauce.data.db.HistoryEntry
import app.sahal.getsauce.data.db.JobStatus
import app.sahal.getsauce.data.prefs.SettingsStore
import app.sahal.getsauce.data.repo.DownloadRepository
import app.sahal.getsauce.merge.FfmpegMerger
import app.sahal.getsauce.ui.MainActivity
import app.sahal.getsauce.util.SafCopy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.atomic.AtomicLong

/**
 * Foreground service that owns the download pipeline. It:
 *   1. reads QUEUED rows from Room in order,
 *   2. sets Go bridge config from the current SettingsStore snapshot,
 *   3. runs ExtractorBridge.download() with a progress listener that batches
 *      byte updates into ~1Hz DB writes so we don't hammer Room,
 *   4. hands the result to FfmpegMerger if needed,
 *   5. copies the finished file into the user's SAF tree,
 *   6. records a HistoryEntry and moves on.
 *
 * A single-worker mutex guards the Go bridge — the CLI-style downloader keeps
 * a lot of config in package globals, so serializing is the safe move.
 */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val runLock = Mutex()
    private var pumpJob: Job? = null
    private var currentJobId: Long? = null

    private lateinit var bridge: ExtractorBridge
    private lateinit var repo: DownloadRepository
    private lateinit var settings: SettingsStore

    override fun onCreate() {
        super.onCreate()
        val container = (applicationContext as App).container
        bridge = container.bridge
        repo = container.repo
        settings = container.settings

        startForeground(NOTIF_ID, buildNotification("Getting ready…", null))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL_CURRENT -> {
                bridge.cancel()
            }
            else -> ensurePump()
        }
        return START_STICKY
    }

    private fun ensurePump() {
        if (pumpJob?.isActive == true) return
        pumpJob = scope.launch {
            runLock.withLock { runPump() }
            stopSelfSoon()
        }
    }

    private suspend fun runPump() {
        while (true) {
            val next = repo.nextQueued() ?: return
            currentJobId = next.id
            try {
                processJob(next)
            } catch (t: Throwable) {
                repo.setStatus(next.id, JobStatus.FAILED, err = t.message ?: t::class.java.simpleName)
                updateNotif(getString(R.string.notif_failed_fmt, next.title), null)
            } finally {
                currentJobId = null
            }
        }
    }

    private suspend fun processJob(job: DownloadJob) {
        // 1. Apply current settings snapshot to the Go bridge.
        val snap = settings.flow.first()
        bridge.setWorkers(snap.workers)
        bridge.setTimeoutMinutes(snap.timeoutMinutes)
        bridge.setUserHeaders(snap.userHeaders)
        bridge.setProxy(snap.proxy)
        bridge.setTruncate(snap.truncate)

        // 2. Mark RUNNING.
        repo.setStatus(job.id, JobStatus.RUNNING)
        updateNotif(getString(R.string.notif_downloading_fmt, job.title), 0)

        // 3. Prepare staging dir.
        val staging = File(cacheDir, "dl/${job.id}").apply { mkdirs() }

        // 4. Batched progress: aggregate deltas, flush at most every 750ms.
        val totalBytes = AtomicLong(job.bytesDone)
        val estimated = if (job.estimatedBytes > 0) job.estimatedBytes else 0L
        val flushEvery = 750L
        var lastFlush = 0L
        val listener = object : BridgeProgressListener() {
            override fun onBytes(delta: Long) {
                val now = System.currentTimeMillis()
                val running = totalBytes.addAndGet(delta)
                if (now - lastFlush >= flushEvery) {
                    lastFlush = now
                    scope.launch {
                        repo.setProgress(job.id, running)
                        val pct = if (estimated > 0) (running * 100 / estimated).toInt().coerceAtMost(100) else null
                        updateNotif(getString(R.string.notif_downloading_fmt, job.title), pct)
                    }
                }
            }
            override fun onLog(msg: String) { /* Log.i(TAG, msg) */ }
        }

        // 5. Run.
        val result = bridge.download(job.payload, job.streamId, staging.absolutePath, listener)

        // 6. Merge (or not).
        val merged: File? = if (result.needsFinalMerge) {
            repo.setStatus(job.id, JobStatus.MERGING)
            updateNotif(getString(R.string.notif_merging_fmt, job.title), null)
            FfmpegMerger(staging).mergeIfNeeded(result)
        } else {
            result.mainFiles.firstOrNull()?.let(::File)
        }

        // 7. Copy into SAF tree if configured, otherwise leave staged.
        val finalUri: String = if (merged != null && job.destTreeUri != null) {
            val name = merged.name
            SafCopy.copyInto(this, Uri.parse(job.destTreeUri), merged, name)?.also {
                runCatching { merged.delete() }
            } ?: merged.absolutePath.also {
                // SAF copy failed; leave file in staging, keep path in history.
            }
        } else {
            merged?.absolutePath ?: run {
                // Multi-file (image set) case: no merge, no single output.
                // We just cite the staging dir.
                staging.absolutePath
            }
        }

        val size = merged?.length() ?: totalBytes.get()

        repo.setStatus(job.id, JobStatus.COMPLETED)
        repo.recordCompletion(
            HistoryEntry(
                sourceUrl = job.sourceUrl,
                title = job.title,
                site = job.site,
                streamLabel = job.streamLabel,
                finalUri = finalUri,
                sizeBytes = size,
                completedAt = System.currentTimeMillis(),
            )
        )
        updateNotif(getString(R.string.notif_done_fmt, job.title), null)
    }

    private fun buildNotification(text: String, progress: Int?): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val cancelIntent = PendingIntent.getService(
            this, 1,
            Intent(this, DownloadService::class.java).setAction(ACTION_CANCEL_CURRENT),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val b = NotificationCompat.Builder(this, App.CHANNEL_DOWNLOADS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.queue_cancel), cancelIntent)
        if (progress != null) b.setProgress(100, progress, false) else b.setProgress(0, 0, true)
        return b.build()
    }

    private fun updateNotif(text: String, progress: Int?) {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        mgr.notify(NOTIF_ID, buildNotification(text, progress))
    }

    private fun stopSelfSoon() {
        val stop = java.lang.Runnable {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
        }
        // Small delay so a rapidly-enqueued follow-up doesn't churn foreground state.
        android.os.Handler(mainLooper).postDelayed(stop, 1500)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIF_ID = 1001
        const val ACTION_CANCEL_CURRENT = "app.sahal.getsauce.action.CANCEL_CURRENT"

        /** Kick the service to look for new work. Safe to call multiple times. */
        fun kick(context: Context) {
            val i = Intent(context, DownloadService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }
    }
}
