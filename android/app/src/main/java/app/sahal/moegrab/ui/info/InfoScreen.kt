package app.sahal.moegrab.ui.info

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.sahal.moegrab.R
import app.sahal.moegrab.bridge.ExtractedData
import app.sahal.moegrab.bridge.ExtractedStream
import app.sahal.moegrab.ui.common.InfoSkeleton
import app.sahal.moegrab.ui.common.rememberVm
import app.sahal.moegrab.util.humanBytes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(url: String, onBack: () -> Unit) {
    val vm: InfoViewModel = rememberVm(key = "info:$url") { InfoViewModel(it, url) }
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.snack) {
        state.snack?.let {
            snackbar.showSnackbar(it)
            vm.clearSnack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.info_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> InfoSkeleton()
                state.error != null -> ErrorView(state.error!!)
                else -> InfoBody(state, vm)
            }
        }
    }
}

@Composable
private fun ErrorView(msg: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.err_extract_fmt, msg),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            "The site may be Cloudflare-protected, geo-blocked, or the URL " +
                "may not match a supported extractor. Try pasting session " +
                "cookies in Settings → User headers.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InfoBody(state: InfoUiState, vm: InfoViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(state.results, key = { _, item -> item.sourceUrl + item.title }) { i, data ->
            // Staggered card entrance: each card fades + slides in slightly later
            // than the previous one. Feels alive without slowing the user down.
            var shown by remember(data) { mutableStateOf(false) }
            LaunchedEffect(data) { shown = true }
            AnimatedVisibility(
                visible = shown,
                enter = fadeIn(tween(400, delayMillis = i * 80)) +
                    slideInVertically(
                        initialOffsetY = { it / 6 },
                        animationSpec = tween(400, delayMillis = i * 80, easing = EaseOutCubic),
                    ),
            ) {
                EntryCard(data, state.selectedStreamId[data.sourceUrl], vm)
            }
        }
    }
}

@Composable
private fun EntryCard(data: ExtractedData, selected: String?, vm: InfoViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(300)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                data.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${data.site}   •   ${data.type}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            data.streams.forEach { s ->
                StreamRow(
                    stream = s,
                    selected = selected == s.id,
                    onSelect = { vm.selectStream(data.sourceUrl, s.id) },
                )
            }

            Text(
                text = if (data.captions.isEmpty()) stringResource(R.string.info_caption_none)
                       else "Captions: " + data.captions.joinToString { it.language },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = { vm.enqueue(data) },
                    enabled = data.streams.isNotEmpty(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Text(stringResource(R.string.info_download), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun StreamRow(
    stream: ExtractedStream,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(Modifier.padding(start = 4.dp)) {
            val label = buildString {
                append(stream.quality.ifBlank { "stream ${stream.id}" })
                if (stream.info.isNotBlank()) append("   •   ${stream.info}")
                if (stream.ext.isNotBlank()) append("   •   .${stream.ext}")
            }
            Text(label, style = MaterialTheme.typography.bodyMedium)
            val sub = buildString {
                if (stream.parts > 0) append("${stream.parts} parts")
                if (stream.size > 0) {
                    if (isNotEmpty()) append("   •   ")
                    append(humanBytes(stream.size))
                }
            }
            if (sub.isNotBlank()) Text(
                sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
