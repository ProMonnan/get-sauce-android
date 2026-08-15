package app.sahal.getsauce.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightScheme = lightColorScheme(
    primary = Color(0xFFB4262A),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF7A5240),
    tertiary = Color(0xFF725B2E),
    background = Color(0xFFFFFBFF),
    surface = Color(0xFFFFFBFF),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFFFB3AE),
    onPrimary = Color(0xFF680012),
    secondary = Color(0xFFECBEA9),
    tertiary = Color(0xFFE1C28A),
    background = Color(0xFF1B1211),
    surface = Color(0xFF1B1211),
)

@Composable
fun GetSauceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val useDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val ctx = LocalContext.current
    val scheme = when {
        useDynamic && darkTheme -> dynamicDarkColorScheme(ctx)
        useDynamic && !darkTheme -> dynamicLightColorScheme(ctx)
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
