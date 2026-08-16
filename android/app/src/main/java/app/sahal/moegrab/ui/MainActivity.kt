package app.sahal.moegrab.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.sahal.moegrab.ui.common.rememberVm
import app.sahal.moegrab.ui.theme.MoeGrabTheme
import app.sahal.moegrab.ui.updater.UpdatePromptSheet
import app.sahal.moegrab.updater.UpdateViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedText = extractSharedText(intent)

        setContent {
            MoeGrabTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val initialUrl = remember { mutableStateOf(sharedText) }

                    // In-app auto-updater (v1.1+). Silent cold-start check;
                    // sheet appears iff a newer release is available.
                    val updateVm: UpdateViewModel = rememberVm { UpdateViewModel(it) }
                    val updateState by updateVm.state.collectAsState()
                    LaunchedEffect(Unit) { updateVm.checkForUpdates(showUpToDate = false) }

                    Box(Modifier.fillMaxSize()) {
                        AppNavHost(
                            initialUrl = initialUrl,
                            onCheckForUpdates = { updateVm.checkForUpdates(showUpToDate = true) },
                        )
                        UpdatePromptSheet(
                            state = updateState,
                            onUpdateNow = { updateVm.downloadAndInstall() },
                            onLater = { updateVm.dismissPrompt() },
                            onDismiss = {
                                updateVm.clearTerminal()
                                updateVm.dismissPrompt()
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun extractSharedText(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        if (intent.type != "text/plain") return null
        return intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
    }
}
