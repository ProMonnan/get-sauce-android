package app.sahal.moegrab.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import app.sahal.moegrab.ui.theme.AccentCream
import app.sahal.moegrab.ui.theme.AccentGold
import app.sahal.moegrab.ui.theme.AccentNavy
import app.sahal.moegrab.ui.theme.AccentRose
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Confetti burst overlay. Renders a fixed set of particles that radiate out
 * from center, spin, then fade. Auto-invokes onFinished after the animation
 * completes so the parent can flip visibility state back off.
 *
 * Cheap on the GPU — just circles + short lines in one Canvas frame per tick.
 */
@Composable
fun ConfettiOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {},
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(120)),
        exit = fadeOut(tween(300)),
    ) {
        // Static particle set — one seed per showing.
        val particles = remember(visible) {
            val rng = Random(System.currentTimeMillis().toInt())
            List(28) {
                Particle(
                    angleDeg = rng.nextInt(360).toFloat(),
                    speed = 220f + rng.nextFloat() * 260f,
                    color = when (rng.nextInt(4)) {
                        0 -> AccentRose
                        1 -> AccentCream
                        2 -> AccentGold
                        else -> AccentNavy
                    },
                    size = 6f + rng.nextFloat() * 10f,
                    spinSpeed = (rng.nextFloat() * 2f - 1f) * 720f,   // deg/sec
                )
            }
        }

        // 0f → 1f progress over ~1.4s
        var run by remember(visible) { mutableStateOf(false) }
        LaunchedEffect(visible) {
            if (visible) {
                run = true
            }
        }
        val progress by animateFloatAsState(
            targetValue = if (run) 1f else 0f,
            animationSpec = tween(1400, easing = LinearEasing),
            label = "confettiProgress",
            finishedListener = { if (it >= 1f) onFinished() },
        )

        Box(modifier = modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                particles.forEach { p ->
                    val t = progress                       // 0..1
                    // easeOut: start fast then decelerate
                    val eased = 1f - (1f - t) * (1f - t)
                    val dist = p.speed * eased
                    val rad = Math.toRadians(p.angleDeg.toDouble())
                    val x = cx + (dist * cos(rad)).toFloat()
                    val y = cy + (dist * sin(rad)).toFloat() + 200f * t * t   // slight gravity
                    val alpha = (1f - t).coerceIn(0f, 1f)
                    val colorA = p.color.copy(alpha = alpha)
                    // draw a short streak (rotated line) so it reads like a ribbon
                    val spin = Math.toRadians((p.spinSpeed * t + p.angleDeg).toDouble())
                    val dx = (p.size * 1.6f * cos(spin)).toFloat()
                    val dy = (p.size * 1.6f * sin(spin)).toFloat()
                    drawLine(
                        color = colorA,
                        start = Offset(x - dx, y - dy),
                        end = Offset(x + dx, y + dy),
                        strokeWidth = p.size * 0.6f,
                    )
                    drawCircle(color = colorA, radius = p.size * 0.35f, center = Offset(x, y))
                }
                // center pulse ring
                val ringT = progress.coerceIn(0f, 0.5f) * 2f
                if (ringT < 1f) {
                    drawCircle(
                        color = AccentRose.copy(alpha = (1f - ringT) * 0.5f),
                        radius = 80f + ringT * 220f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 6f),
                    )
                }
            }
        }
    }
}

private data class Particle(
    val angleDeg: Float,
    val speed: Float,
    val color: Color,
    val size: Float,
    val spinSpeed: Float,
)
