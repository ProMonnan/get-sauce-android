package app.sahal.moegrab.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.sahal.moegrab.R
import app.sahal.moegrab.ui.common.MoeTopBar
import app.sahal.moegrab.ui.common.rememberVm
import app.sahal.moegrab.util.takePersistablePermissions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val vm: SettingsViewModel = rememberVm { SettingsViewModel(it) }
    val snap by vm.snapshot.collectAsState()
    val ctx = LocalContext.current

    val treePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            takePersistablePermissions(ctx.contentResolver, uri)
            vm.setOutputTree(uri.toString())
        }
    }

    Scaffold(topBar = { MoeTopBar(title = stringResource(R.string.settings_title)) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Output folder
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.settings_output), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = snap.outputTreeUri?.let { display(it) } ?: "(not set)",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = {
                        treePicker.launch(startPickerHint())
                    }) { Text(stringResource(R.string.settings_output_pick)) }
                }
            }

            HorizontalDivider()

            // Workers slider
            Column {
                Text("${stringResource(R.string.settings_workers)}: ${snap.workers}",
                    style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = snap.workers.toFloat(),
                    onValueChange = { vm.setWorkers(it.toInt()) },
                    valueRange = 1f..16f,
                    steps = 14,
                )
            }

            // Timeout slider
            Column {
                Text("${stringResource(R.string.settings_timeout)}: ${snap.timeoutMinutes}",
                    style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = snap.timeoutMinutes.toFloat(),
                    onValueChange = { vm.setTimeout(it.toInt()) },
                    valueRange = 1f..60f,
                    steps = 58,
                )
            }

            HorizontalDivider()

            OutlinedTextField(
                value = snap.proxy,
                onValueChange = vm::setProxy,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_proxy)) },
                placeholder = { Text("http://user:pass@host:port") },
            )

            Column {
                OutlinedTextField(
                    value = snap.userHeaders,
                    onValueChange = vm::setUserHeaders,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_headers)) },
                    placeholder = { Text("Cookie: cf_clearance=...\nUser-Agent: Mozilla/5.0 …") },
                    minLines = 3,
                    maxLines = 8,
                )
                Text(stringResource(R.string.settings_headers_hint), style = MaterialTheme.typography.bodySmall)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.settings_truncate), modifier = Modifier.weight(1f))
                Switch(checked = snap.truncate, onCheckedChange = vm::setTruncate)
            }
        }
    }
}

private fun startPickerHint(): android.net.Uri? = try {
    // Deep-link into Documents by default; the user can navigate elsewhere.
    "content://com.android.externalstorage.documents/document/primary%3ADownload".toUri()
} catch (_: Throwable) { null }

private fun display(treeUri: String): String = treeUri
    .substringAfterLast("tree/")
    .let { java.net.URLDecoder.decode(it, "UTF-8") }
