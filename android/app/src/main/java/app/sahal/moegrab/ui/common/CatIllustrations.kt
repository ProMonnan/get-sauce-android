package app.sahal.moegrab.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A curled-up, sleeping chibi cat, drawn with vector paths. Sizes flexibly to
 * the modifier's dimensions. Three animated "Zzz" float above the head with a
 * gentle up-down bob so the empty state doesn't feel static.
 */
@Composable
fun SleepingCatIllustration(modifier: Modifier = Modifier) {
    val fill = MaterialTheme.colorScheme.primary
    val accent = MaterialTheme.colorScheme.tertiary
    val zColor = MaterialTheme.colorScheme.onSurfaceVariant

    // gentle "breathing" bob for the Zs
    val transition = rememberInfiniteTransition(label = "catBob")
    val zBob by transition.animateFloat(
        initialValue = 0f, targetValue = 6f,
        animationSpec = infiniteRepeatable(
            tween(1800), repeatMode = RepeatMode.Reverse,
        ),
        label = "catZBob",
    )

    val measurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h * 0.60f       // curled body sits slightly below center
        val bodyR = minOf(w, h) * 0.36f

        // --- body (curled circle) ------------------------------------------
        drawCircle(color = fill, radius = bodyR, center = Offset(cx, cy))

        // --- tail (curved comma looping around the back) -------------------
        val tail = Path().apply {
            moveTo(cx + bodyR * 0.85f, cy)
            cubicTo(
                cx + bodyR * 1.4f, cy - bodyR * 0.2f,
                cx + bodyR * 1.4f, cy + bodyR * 0.6f,
                cx + bodyR * 0.9f, cy + bodyR * 0.55f,
            )
        }
        drawPath(tail, color = fill, style = Stroke(width = bodyR * 0.35f))

        // --- head (smaller circle up-left) ---------------------------------
        val headR = bodyR * 0.62f
        val headCx = cx - bodyR * 0.35f
        val headCy = cy - bodyR * 0.55f
        drawCircle(color = fill, radius = headR, center = Offset(headCx, headCy))

        // --- ears (two triangles) ------------------------------------------
        val earH = headR * 0.65f
        val earW = headR * 0.55f
        val leftEar = Path().apply {
            moveTo(headCx - headR * 0.6f, headCy - headR * 0.5f)
            lineTo(headCx - headR * 0.85f, headCy - headR * 0.5f - earH)
            lineTo(headCx - headR * 0.2f, headCy - headR * 0.85f)
            close()
        }
        val rightEar = Path().apply {
            moveTo(headCx + headR * 0.1f, headCy - headR * 0.85f)
            lineTo(headCx + headR * 0.35f, headCy - headR * 0.5f - earH)
            lineTo(headCx + headR * 0.5f, headCy - headR * 0.4f)
            close()
        }
        drawPath(leftEar, color = fill)
        drawPath(rightEar, color = fill)

        // --- closed eyes (two small arcs like ‿ rotated to look shut) ------
        val eyeY = headCy - headR * 0.05f
        val eyeW = headR * 0.28f
        val stroke = Stroke(width = headR * 0.09f)
        val leftEye = Path().apply {
            moveTo(headCx - headR * 0.35f - eyeW / 2, eyeY)
            quadraticBezierTo(headCx - headR * 0.35f, eyeY + eyeW * 0.6f, headCx - headR * 0.35f + eyeW / 2, eyeY)
        }
        val rightEye = Path().apply {
            moveTo(headCx + headR * 0.15f - eyeW / 2, eyeY)
            quadraticBezierTo(headCx + headR * 0.15f, eyeY + eyeW * 0.6f, headCx + headR * 0.15f + eyeW / 2, eyeY)
        }
        drawPath(leftEye, color = accent, style = stroke)
        drawPath(rightEye, color = accent, style = stroke)

        // --- little nose (triangle) ----------------------------------------
        val nose = Path().apply {
            moveTo(headCx - headR * 0.11f, eyeY + headR * 0.28f)
            lineTo(headCx - headR * -0.01f, eyeY + headR * 0.45f)
            lineTo(headCx + headR * 0.09f, eyeY + headR * 0.28f)
            close()
        }
        drawPath(nose, color = accent)

        // --- Zzz floating above (three sizes, bobbing) ---------------------
        val zBase = Offset(headCx + headR * 1.05f, headCy - headR * 0.85f - zBob)
        listOf(
            Triple(0f,    0f,    26f),
            Triple(28f, -22f,   20f),
            Triple(50f, -42f,   14f),
        ).forEach { (dx, dy, sz) ->
            val layout = measurer.measure(
                text = androidx.compose.ui.text.AnnotatedString("Z"),
                style = TextStyle(fontSize = sz.sp, fontWeight = FontWeight.Bold, color = zColor),
            )
            drawText(layout, topLeft = Offset(zBase.x + dx, zBase.y + dy))
        }
    }
}

/**
 * A stylized "empty box" — used for empty history state. Simpler than the cat.
 */
@Composable
fun EmptyBoxIllustration(modifier: Modifier = Modifier) {
    val stroke = MaterialTheme.colorScheme.outline
    val fill = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val boxW = w * 0.7f
        val boxH = h * 0.5f
        val boxX = (w - boxW) / 2
        val boxY = h - boxH - h * 0.1f
        // shadow ellipse
        drawOval(
            color = stroke.copy(alpha = 0.15f),
            topLeft = Offset(boxX - boxW * 0.05f, boxY + boxH * 1.02f),
            size = Size(boxW * 1.1f, boxH * 0.18f),
        )
        // box body
        drawRoundRect(
            color = fill,
            topLeft = Offset(boxX, boxY),
            size = Size(boxW, boxH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(boxW * 0.06f, boxW * 0.06f),
        )
        drawRoundRect(
            color = stroke,
            topLeft = Offset(boxX, boxY),
            size = Size(boxW, boxH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(boxW * 0.06f, boxW * 0.06f),
            style = Stroke(width = 4f),
        )
        // box seam
        drawLine(
            color = stroke,
            start = Offset(boxX, boxY + boxH * 0.28f),
            end = Offset(boxX + boxW, boxY + boxH * 0.28f),
            strokeWidth = 4f,
        )
        // little sparkle above
        val sparkleY = boxY - h * 0.05f
        drawLine(stroke, Offset(w / 2 - 20f, sparkleY), Offset(w / 2 + 20f, sparkleY), 3f)
        drawLine(stroke, Offset(w / 2, sparkleY - 20f), Offset(w / 2, sparkleY + 20f), 3f)
    }
}
