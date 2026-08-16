package app.sahal.moegrab.ui.player

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * Full-screen in-app video player using Media3 / ExoPlayer.
 *
 * Path/URI comes from the History entry the user tapped. We accept both
 * `content://` (SAF-backed final output) and `file://` / plain paths
 * (staged files that never got copied to a SAF tree).
 *
 * The player is owned by this composable and released in DisposableEffect's
 * cleanup — no shared ViewModel because a player instance mid-play doesn't
 * survive Activity recreation the way a Room-backed VM would anyway.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(uri: String, title: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(ctx).build().apply {
            val mediaUri = try {
                val parsed = Uri.parse(uri)
                if (parsed.scheme.isNullOrBlank()) Uri.fromFile(java.io.File(uri)) else parsed
            } catch (_: Throwable) {
                Uri.parse(uri)
            }
            setMediaItem(MediaItem.fromUri(mediaUri))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    titleContentColor = Color.White,
                    scrolledContainerColor = Color.Black.copy(alpha = 0.6f),
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { c ->
                    PlayerView(c).apply {
                        player = exoPlayer
                        useController = true
                        controllerAutoShow = true
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                    }
                },
            )
            // Top-app-bar sits over the player so we don't crop the video with
            // its inset padding; consume the padding by NOT applying it to the
            // AndroidView. The bar's semi-transparent bg keeps the title
            // readable without stealing pixels from the frame.
            @Suppress("UNUSED_EXPRESSION") padding
        }
    }
}
