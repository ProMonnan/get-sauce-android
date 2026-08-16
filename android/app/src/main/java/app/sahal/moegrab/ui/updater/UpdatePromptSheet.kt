package app.sahal.moegrab.ui.updater

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.sahal.moegrab.updater.Phase
import app.sahal.moegrab.updater.UpdateState
import app.sahal.moegrab.util.humanBytes

/**
 * Bottom-sheet prompt for the updater flow. Renders different content per
 * phase — AVAILABLE (with release notes), DOWNLOADING (with progress),
 * READY_TO_INSTALL (waiting for system installer), FAILED, UP_TO_DATE.
 *
 * The sheet is visible whenever the phase is one of the four "user-facing"
 * ones; IDLE / CHECKING keep the sheet closed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatePromptSheet(
    state: UpdateState,
    onUpdateNow: () -> Unit,
    onLater: () -> Unit,
    onDismiss: () -> Unit,
) {
    val visible = state.phase in setOf(
        Phase.AVAILABLE, Phase.DOWNLOADING, Phase.READY_TO_INSTALL,
        Phase.FAILED, Phase.UP_TO_DATE,
    )
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (state.phase) {
                Phase.AVAILABLE          -> AvailableContent(state, onUpdateNow, onLater)
                Phase.DOWNLOADING        -> DownloadingContent(state)
                Phase.READY_TO_INSTALL   -> ReadyToInstallContent()
                Phase.FAILED             -> FailedContent(state, onLater)
                Phase.UP_TO_DATE         -> UpToDateContent(onLater)
                else                     -> Unit
            }
        }
    }
}

@Composable
private fun AvailableContent(state: UpdateState, onUpdate: () -> Unit, onLater: () -> Unit) {
    val info = state.info ?: return
    HeaderRow(
        icon = { HeaderIcon(Icons.Filled.NewReleases, MaterialTheme.colorScheme.primary) },
        title = "Update available",
        subtitle = "v${info.version}  •  ${humanBytes(info.sizeBytes)}",
    )
    ReleaseNotes(info.releaseNotes)
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onLater) { Text("Later") }
        Spacer(Modifier.size(8.dp))
        Button(onClick = onUpdate, shape = MaterialTheme.shapes.medium) {
            Icon(Icons.Filled.Download, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Update now")
        }
    }
}

@Composable
private fun DownloadingContent(state: UpdateState) {
    val info = state.info ?: return
    HeaderRow(
        icon = { HeaderIcon(Icons.Filled.Download, MaterialTheme.colorScheme.primary) },
        title = "Downloading v${info.version}",
        subtitle = "${humanBytes(state.bytesDone)} / ${humanBytes(state.bytesTotal)}",
    )
    val progress = if (state.bytesTotal > 0)
        (state.bytesDone.toFloat() / state.bytesTotal.toFloat()).coerceIn(0f, 1f)
    else 0f
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
    Text(
        "Once download finishes, Android's system installer will open — tap Install to complete the update.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ReadyToInstallContent() {
    HeaderRow(
        icon = { HeaderIcon(Icons.Filled.CheckCircle, MaterialTheme.colorScheme.primary) },
        title = "Ready to install",
        subtitle = "Tap Install on the system prompt.",
    )
    Text(
        "If you don't see the installer, pull down the notification shade — Android may have queued it there.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FailedContent(state: UpdateState, onDismiss: () -> Unit) {
    HeaderRow(
        icon = { HeaderIcon(Icons.Filled.ErrorOutline, MaterialTheme.colorScheme.error) },
        title = "Update failed",
        subtitle = state.error ?: "Unknown error",
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onDismiss) { Text("Dismiss") }
    }
}

@Composable
private fun UpToDateContent(onDismiss: () -> Unit) {
    HeaderRow(
        icon = { HeaderIcon(Icons.Filled.CheckCircle, MaterialTheme.colorScheme.primary) },
        title = "You're up to date",
        subtitle = "No newer release published yet.",
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onDismiss) { Text("OK") }
    }
}

// ---- shared bits ---------------------------------------------------------

@Composable
private fun HeaderRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(Modifier.size(12.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HeaderIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(36.dp))
    }
}

@Composable
private fun ReleaseNotes(body: String) {
    // Release-notes body is markdown; we render it as plain text (readable
    // enough for a changelog snippet). If the body is empty, show a soft
    // placeholder rather than an empty box.
    val text = body.trim().ifBlank { "See the release page on GitHub for details." }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp)
            .clip(MaterialTheme.shapes.small),
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
