package app.sahal.getsauce.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.sahal.getsauce.R

/**
 * The entry point. Kept intentionally minimal — a URL box, a Fetch button, and
 * a link to the supported-sites reference. Everything else lives on Info/Queue.
 */
@Composable
fun HomeScreen(
    initialUrl: String?,
    onUrlConsumed: () -> Unit,
    onNavigateInfo: (String) -> Unit,
    onNavigateSites: () -> Unit,
) {
    var text by remember { mutableStateOf(initialUrl.orEmpty()) }

    // Consume the shared-intent URL after applying it so a back-and-forward
    // to Home doesn't refill the box unexpectedly.
    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank()) {
            text = initialUrl
            onUrlConsumed()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = false,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.home_hint)) },
        )
        Button(
            onClick = { if (text.isNotBlank()) onNavigateInfo(text.trim()) },
            enabled = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.home_extract)) }

        OutlinedButton(
            onClick = onNavigateSites,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.home_supported_sites)) }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "Tip: bypass Cloudflare pages by pasting cookies from a real browser session " +
                "into Settings → User headers.",
            textAlign = TextAlign.Start,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        )
    }
}
