package app.sahal.moegrab.ui.common

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

/**
 * MoeGrab top bar. Center-aligned title, TRANSPARENT container so it visually
 * merges with the background/status bar (no ugly color-band). Kept as a shared
 * helper so every screen picks up the same styling without repetition.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoeTopBar(
    title: String,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable (androidx.compose.foundation.layout.RowScope.() -> Unit) = {},
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        },
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
    )
}
