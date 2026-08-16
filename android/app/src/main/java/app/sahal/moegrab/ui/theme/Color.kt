package app.sahal.moegrab.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * MoeGrab palette. Sampled from the launcher icon (navy background + rose /
 * cream cat) and expanded into a full Material 3 tonal system.
 *
 *   Anchor colors from the icon (sampled at #F0D8D8, #F0A890, #000030, ...):
 *     rose      — cat body / bow highlights
 *     cream     — cat face / muzzle
 *     navy      — background gradient
 *
 * Both schemes use these anchors as primary/secondary/tertiary respectively,
 * chosen so the identity carries across day + night without dynamic color.
 */

// --- Light scheme -----------------------------------------------------------

val md_light_primary            = Color(0xFFB5405C)  // deep rose, WCAG-AA on white
val md_light_onPrimary          = Color(0xFFFFFFFF)
val md_light_primaryContainer   = Color(0xFFFFD9E1)
val md_light_onPrimaryContainer = Color(0xFF3F0016)

val md_light_secondary            = Color(0xFF775753)  // warm brown from cream shading
val md_light_onSecondary          = Color(0xFFFFFFFF)
val md_light_secondaryContainer   = Color(0xFFFFDAD5)
val md_light_onSecondaryContainer = Color(0xFF2C1512)

val md_light_tertiary            = Color(0xFF3A3E70)  // navy, tinted for readability
val md_light_onTertiary          = Color(0xFFFFFFFF)
val md_light_tertiaryContainer   = Color(0xFFDEE0FF)
val md_light_onTertiaryContainer = Color(0xFF000A64)

val md_light_error            = Color(0xFFBA1A1A)
val md_light_onError          = Color(0xFFFFFFFF)
val md_light_errorContainer   = Color(0xFFFFDAD6)
val md_light_onErrorContainer = Color(0xFF410002)

val md_light_background     = Color(0xFFFFF8F9)   // pink-tinted white
val md_light_onBackground   = Color(0xFF201A1B)
val md_light_surface        = Color(0xFFFFFFFF)
val md_light_onSurface      = Color(0xFF201A1B)
val md_light_surfaceVariant = Color(0xFFF5EAED)
val md_light_onSurfaceVariant = Color(0xFF524345)
val md_light_outline        = Color(0xFF84747A)
val md_light_outlineVariant = Color(0xFFD7C2C7)
val md_light_scrim          = Color(0xFF000000)

// --- Dark scheme (owns the identity — matches the icon) --------------------

val md_dark_primary            = Color(0xFFFFB3C1)   // soft rose, glows on navy
val md_dark_onPrimary          = Color(0xFF5D1029)
val md_dark_primaryContainer   = Color(0xFF7E293F)
val md_dark_onPrimaryContainer = Color(0xFFFFD9E1)

val md_dark_secondary            = Color(0xFFF5E6D3)  // cream — the cat's face color
val md_dark_onSecondary          = Color(0xFF3A2B1F)
val md_dark_secondaryContainer   = Color(0xFF513F32)
val md_dark_onSecondaryContainer = Color(0xFFFFDCC0)

val md_dark_tertiary            = Color(0xFFC0C3FF)   // soft lavender/navy accent
val md_dark_onTertiary          = Color(0xFF0F1670)
val md_dark_tertiaryContainer   = Color(0xFF282E88)
val md_dark_onTertiaryContainer = Color(0xFFDEE0FF)

val md_dark_error            = Color(0xFFFFB4AB)
val md_dark_onError          = Color(0xFF690005)
val md_dark_errorContainer   = Color(0xFF93000A)
val md_dark_onErrorContainer = Color(0xFFFFDAD6)

val md_dark_background     = Color(0xFF0F0F2E)      // navy from icon
val md_dark_onBackground   = Color(0xFFEDDFE1)
val md_dark_surface        = Color(0xFF161638)      // icon adaptive bg
val md_dark_onSurface      = Color(0xFFEDDFE1)
val md_dark_surfaceVariant = Color(0xFF1F1F45)
val md_dark_onSurfaceVariant = Color(0xFFD6C2C6)
val md_dark_outline        = Color(0xFF5A5478)
val md_dark_outlineVariant = Color(0xFF302C4A)
val md_dark_scrim          = Color(0xFF000000)

// --- Success accents (used by the download-complete confetti animation) ----

val AccentRose  = Color(0xFFF06E88)
val AccentCream = Color(0xFFF5E6D3)
val AccentNavy  = Color(0xFF3A3E70)
val AccentGold  = Color(0xFFF0C878)
