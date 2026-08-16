package app.sahal.moegrab.ui.queue

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.sahal.moegrab.R
import app.sahal.moegrab.data.db.DownloadJob
import app.sahal.moegrab.data.db.JobStatus
import app.sahal.moegrab.ui.common.rememberVm
import app.sahal.moegrab.util.humanBytes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen() {
    val vm: QueueViewModel = rememberVm { QueueViewModel(it) }
    val jobs by vm.jobs.collectAsState(initial = emptyList())

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.queue_title)) }) }) { padding ->
        if (jobs.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), Alignment.Center) {
                Text(stringResource(R.string.queue_empty))
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(jobs, key = { it.id }) { job ->
                    JobRow(job, onCancel = { vm.cancel(job.id) }, onDelete = { vm.delete(job.id) })
                }
            }
        }
    }
}

@Composable
private fun JobRow(job: DownloadJob, onCancel: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(job.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${job.site} • ${job.streamLabel}", style = MaterialTheme.typography.bodySmall)

            val progress = if (job.estimatedBytes > 0)
                (job.bytesDone.toFloat() / job.estimatedBytes.toFloat()).coerceIn(0f, 1f)
            else null

            if (progress != null) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            } else if (job.status == JobStatus.RUNNING || job.status == JobStatus.MERGING) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val bytesStr = if (job.estimatedBytes > 0)
                    "${humanBytes(job.bytesDone)} / ${humanBytes(job.estimatedBytes)}"
                else humanBytes(job.bytesDone)
                Text("${job.status} • $bytesStr", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))

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
