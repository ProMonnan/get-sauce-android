package app.sahal.moegrab.ui.history

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.sahal.moegrab.R
import app.sahal.moegrab.data.db.HistoryEntry
import app.sahal.moegrab.ui.common.EmptyBoxIllustration
import app.sahal.moegrab.ui.common.EmptyState
import app.sahal.moegrab.ui.common.rememberVm
import app.sahal.moegrab.util.humanBytes
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val vm: HistoryViewModel = rememberVm { HistoryViewModel(it) }
    val entries by vm.history.collectAsState(initial = emptyList())
    val ctx = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                actions = {
                    IconButton(onClick = { vm.clear() }) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = stringResource(R.string.history_clear))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (entries.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.history_empty),
                    subtitle = "Finished downloads will show up here.",
                    illustration = { EmptyBoxIllustration(Modifier.size(180.dp)) },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(entries, key = { it.id }) { e ->
                        EntryCard(e, onOpen = {
                            val uri = android.net.Uri.parse(e.finalUri)
                            if (uri.scheme == "content") {
                                val i = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "video/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                runCatching { ctx.startActivity(Intent.createChooser(i, e.title)) }
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryCard(entry: HistoryEntry, onOpen: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .animateContentSize(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                entry.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${entry.site} • ${entry.streamLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${humanBytes(entry.sizeBytes)} • ${DateFormat.getDateTimeInstance().format(Date(entry.completedAt))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
