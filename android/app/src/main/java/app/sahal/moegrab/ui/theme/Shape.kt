package app.sahal.moegrab.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * MoeGrab shape system. Rounder than Material defaults — a chibi mascot
 * deserves soft corners. All values still respect the M3 shape tokens so
 * components pick up the right radius by role.
 */
val MoeGrabShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),   // Cards, dialogs
    large = RoundedCornerShape(24.dp),    // Bottom sheets
    extraLarge = RoundedCornerShape(32.dp),
)
