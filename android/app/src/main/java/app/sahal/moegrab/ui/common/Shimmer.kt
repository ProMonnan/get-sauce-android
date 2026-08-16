package app.sahal.moegrab.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Diagonal shimmer sweep. Apply to any modifier chain to give a skeleton
 * placeholder that "breathes." Pulls colors from the current color scheme
 * so it flows in both light and dark modes without extra config.
 */
fun Modifier.shimmer(shape: Shape = RectangleShape): Modifier = composed {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)

    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerOffset",
    )

    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(offset, 0f),
        end = Offset(offset + 600f, 600f),
    )
    this.clip(shape).background(brush)
}

/**
 * Ready-made skeleton for the InfoScreen: three fake "video cards" with
 * a title-bar, meta-line, three stream rows, and a download button silhouette.
 */
@Composable
fun InfoSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(2) { SkeletonCard() }
    }
}

@Composable
private fun SkeletonCard() {
    val cardShape = MaterialTheme.shapes.medium
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SkeletonLine(fraction = 0.85f, height = 20.dp)
            SkeletonLine(fraction = 0.45f, height = 12.dp)
            Spacer(Modifier.height(4.dp))
            repeat(3) {
                SkeletonLine(fraction = 0.7f, height = 16.dp)
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .height(38.dp)
                    .shimmer(MaterialTheme.shapes.small),
            )
        }
    }
}

@Composable
private fun SkeletonLine(fraction: Float, height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth(fraction)
            .height(height)
            .shimmer(MaterialTheme.shapes.extraSmall),
    )
}

@Suppress("unused")
private val _colorPreviewHint = Color.Unspecified   // keeps the Color import used even if inlined later
