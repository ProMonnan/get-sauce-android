package app.sahal.moegrab.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.sahal.moegrab.R

/**
 * Home. Entry point: URL box + Fetch button + Sites link. Wrapped in a manual
 * status-bar-inset padding so it sits below the system clock cleanly (there is
 * no TopAppBar on this screen — the hero header IS the top decoration).
 */
@Composable
fun HomeScreen(
    initialUrl: String?,
    onUrlConsumed: () -> Unit,
    onNavigateInfo: (String) -> Unit,
    onNavigateSites: () -> Unit,
) {
    var text by remember { mutableStateOf(initialUrl.orEmpty()) }

    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank()) {
            text = initialUrl
            onUrlConsumed()
        }
    }

    // reveal-in animation on first mount
    var mounted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { mounted = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AnimatedVisibility(
            visible = mounted,
            enter = fadeIn(tween(500)) + slideInVertically(
                initialOffsetY = { -it / 3 },
                animationSpec = tween(500, easing = EaseOutCubic),
            ),
        ) {
            HeroHeader()
        }

        AnimatedVisibility(
            visible = mounted,
            enter = fadeIn(tween(700, delayMillis = 120)) + slideInVertically(
                initialOffsetY = { it / 4 },
                animationSpec = tween(600, delayMillis = 120, easing = EaseOutCubic),
            ),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.home_hint)) },
                    leadingIcon = { Icon(Icons.Filled.ContentPaste, contentDescription = null) },
                    shape = MaterialTheme.shapes.medium,
                )
                Button(
                    onClick = { if (text.isNotBlank()) onNavigateInfo(text.trim()) },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(Icons.Filled.Bolt, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        stringResource(R.string.home_extract),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                OutlinedButton(
                    onClick = onNavigateSites,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(Icons.Filled.List, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.home_supported_sites))
                }
            }
        }

        Spacer(Modifier.height(2.dp))

        AnimatedVisibility(
            visible = mounted,
            enter = fadeIn(tween(700, delayMillis = 260)),
        ) {
            TipCard()
        }
    }
}

@Composable
private fun HeroHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "★",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.size(12.dp))
        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Grab it. Save it. Watch it later.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TipCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Cloudflare-protected page?",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                "Paste cookies from a real browser session into Settings → User headers.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
