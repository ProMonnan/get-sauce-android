package app.sahal.moegrab.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * MoeGrab typography. We stay on the system sans (no bundled font binary yet)
 * but push weight + tracking so display / headline styles feel distinctive
 * instead of generic. A proper display face (Rubik) can drop in later —
 * only this file needs to change.
 */

// Small helper so all our display styles share the same "brand" tuning.
private fun brand(
    size: Int,
    weight: FontWeight,
    lineHeight: Int,
    tracking: Double = -0.02,   // slightly tighter than Material defaults
) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
)

val MoeGrabTypography = Typography(
    // Display + headline: extra-bold, tight tracking → looks like a "logo"
    displayLarge   = brand(size = 57, weight = FontWeight.Black,    lineHeight = 64, tracking = -0.03),
    displayMedium  = brand(size = 45, weight = FontWeight.ExtraBold, lineHeight = 52, tracking = -0.03),
    displaySmall   = brand(size = 36, weight = FontWeight.ExtraBold, lineHeight = 44),
    headlineLarge  = brand(size = 32, weight = FontWeight.Bold,      lineHeight = 40),
    headlineMedium = brand(size = 28, weight = FontWeight.Bold,      lineHeight = 36),
    headlineSmall  = brand(size = 24, weight = FontWeight.Bold,      lineHeight = 32),

    // Title: semi-bold, mostly neutral tracking — sits on cards / app bars
    titleLarge  = brand(size = 22, weight = FontWeight.SemiBold, lineHeight = 28, tracking = 0.0),
    titleMedium = brand(size = 16, weight = FontWeight.SemiBold, lineHeight = 24, tracking = 0.005),
    titleSmall  = brand(size = 14, weight = FontWeight.SemiBold, lineHeight = 20, tracking = 0.005),

    // Body + label: keep Material defaults — they're already well-tuned.
    bodyLarge  = Typography().bodyLarge,
    bodyMedium = Typography().bodyMedium,
    bodySmall  = Typography().bodySmall,
    labelLarge  = Typography().labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = Typography().labelMedium.copy(fontWeight = FontWeight.SemiBold),
    labelSmall  = Typography().labelSmall.copy(fontWeight = FontWeight.SemiBold),
)
