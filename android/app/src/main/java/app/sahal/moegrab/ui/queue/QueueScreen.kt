package app.sahal.moegrab.ui.queue

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.sahal.moegrab.R
import app.sahal.moegrab.data.db.DownloadJob
import app.sahal.moegrab.data.db.JobStatus
import app.sahal.moegrab.ui.common.ConfettiOverlay
import app.sahal.moegrab.ui.common.EmptyState
import app.sahal.moegrab.ui.common.MoeTopBar
import app.sahal.moegrab.ui.common.rememberVm
import app.sahal.moegrab.util.humanBytes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen() {
    val vm: QueueViewModel = rememberVm { QueueViewModel(it) }
    val jobs by vm.jobs.collectAsState(initial = emptyList())

    // Fire confetti when a job flips into COMPLETED — track the highest-seen
    // completed-count and celebrate each increment.
    var seenCompletedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var confettiVisible by remember { mutableStateOf(false) }
    var firstFrame by remember { mutableStateOf(true) }
    LaunchedEffect(jobs) {
        val completedNow = jobs.filter { it.status == JobStatus.COMPLETED }.map { it.id }.toSet()
        val newlyCompleted = completedNow - seenCompletedIds
        if (newlyCompleted.isNotEmpty() && !firstFrame) {
            confettiVisible = true
        }
        seenCompletedIds = completedNow
        firstFrame = false
    }

    Scaffold(topBar = { MoeTopBar(title = stringResource(R.string.queue_title)) }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (jobs.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.queue_empty),
                    subtitle = "Paste a URL on the Home tab to start a download.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(jobs, key = { it.id }) { job ->
                        JobRow(
                            job = job,
                            onCancel = { vm.cancel(job.id) },
                            onDelete = { vm.delete(job.id) },
                        )
                    }
                }
            }

            ConfettiOverlay(
                visible = confettiVisible,
                onFinished = { confettiVisible = false },
            )
        }
    }
}

@Composable
private fun JobRow(
    job: DownloadJob,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (statusIcon, statusColor) = statusVisual(job.status)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (job.status == JobStatus.COMPLETED)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    job.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                "${job.site} • ${job.streamLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val progress = if (job.estimatedBytes > 0)
                (job.bytesDone.toFloat() / job.estimatedBytes.toFloat()).coerceIn(0f, 1f)
            else null

            when {
                progress != null && job.status != JobStatus.COMPLETED ->
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                job.status == JobStatus.RUNNING || job.status == JobStatus.MERGING ->
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                else -> Unit
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val bytesStr = if (job.estimatedBytes > 0)
                    "${humanBytes(job.bytesDone)} / ${humanBytes(job.estimatedBytes)}"
                else humanBytes(job.bytesDone)
                // Live speed only makes sense while actively downloading — merging
                // + queued rows keep the bytes label but hide the speed.
                val speed = rememberDownloadSpeed(job.bytesDone, job.status)
                val speedStr = if (speed > 0 && job.status == JobStatus.RUNNING)
                    "  •  ${humanBytes(speed)}/s"
                else ""
                Text(
                    "${job.status} • $bytesStr$speedStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )

                when (job.status) {
                    JobStatus.QUEUED, JobStatus.RUNNING, JobStatus.MERGING ->
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Filled.Cancel, contentDescription = stringResource(R.string.queue_cancel))
                        }
                    else -> IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                    }
                }
            }

            job.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/**
 * Compute a smoothed bytes-per-second rate from consecutive `bytesDone`
 * samples. Updates at most every ~500ms so the label doesn't jitter.
 * Returns 0 when the job isn't RUNNING or we don't have two samples yet.
 *
 * Kept as a scoped composable-state hook so each JobRow keeps its own
 * history — no shared ViewModel state, no cross-job leakage.
 */
@Composable
private fun rememberDownloadSpeed(bytesDone: Long, status: JobStatus): Long {
    var speed by remember { mutableStateOf(0L) }
    var lastBytes by remember { mutableStateOf(bytesDone) }
    var lastAtMs by remember { mutableStateOf(0L) }

    LaunchedEffect(bytesDone, status) {
        if (status != JobStatus.RUNNING) {
            speed = 0L
            lastBytes = bytesDone
            lastAtMs = 0L
            return@LaunchedEffect
        }
        val now = System.currentTimeMillis()
        if (lastAtMs == 0L) {
            lastAtMs = now
            lastBytes = bytesDone
            return@LaunchedEffect
        }
        val elapsed = now - lastAtMs
        if (elapsed < 500) return@LaunchedEffect
        val delta = bytesDone - lastBytes
        if (delta >= 0) {
            // Exponential smoothing (0.6 new / 0.4 old) so a burst doesn't spike
            // the label and a stall doesn't collapse it to 0 for a second.
            val instant = (delta * 1000L) / elapsed
            speed = if (speed == 0L) instant else ((instant * 6 + speed * 4) / 10)
        }
        lastBytes = bytesDone
        lastAtMs = now
    }
    return speed
}

@Composable
private fun statusVisual(status: JobStatus): Pair<ImageVector, androidx.compose.ui.graphics.Color> = when (status) {
    JobStatus.QUEUED    -> Icons.Filled.HourglassEmpty to MaterialTheme.colorScheme.onSurfaceVariant
    JobStatus.RUNNING   -> Icons.Filled.Downloading   to MaterialTheme.colorScheme.primary
    JobStatus.MERGING   -> Icons.Filled.MergeType     to MaterialTheme.colorScheme.tertiary
    JobStatus.COMPLETED -> Icons.Filled.CheckCircle   to MaterialTheme.colorScheme.primary
    JobStatus.FAILED    -> Icons.Filled.ErrorOutline  to MaterialTheme.colorScheme.error
    JobStatus.CANCELLED -> Icons.Filled.Cancel        to MaterialTheme.colorScheme.onSurfaceVariant
}
