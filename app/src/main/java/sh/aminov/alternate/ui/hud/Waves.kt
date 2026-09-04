package sh.aminov.alternate.ui.hud

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import sh.aminov.alternate.keypad.KeypadVisuals
import sh.aminov.alternate.ui.theme.Accent
import sh.aminov.alternate.ui.theme.Bone
import sh.aminov.alternate.ui.theme.Void
import kotlin.math.asin

/**
 * The core and the waves radiating from it.
 *
 * Each tap releases a wave in its own direction, so the alternation is visible
 * in peripheral vision without looking away from the centre. Perspective rests
 * on three cues moving together: as it recedes the wave shrinks, loses
 * curvature and fades — one cue alone reads as "dimmer", three read as
 * "farther". The neon is layered as a wide dim halo, a bright outline and a
 * thin white vein, which looks like a glowing tube for three draw calls.
 */
fun DrawScope.drawWaves(g: HudGeometry, v: KeypadVisuals) {
    val waves = v.waves
    for (i in 0 until waves.capacity) {
        if (!waves.isAlive(i)) continue

        val progress = waves.progress(i)
        // Shockwave: fast off the mark, almost stopped by the end of its path.
        val remaining = 1f - progress
        val travel = 1f - EASE_QUADRATIC * remaining * remaining - EASE_LINEAR * remaining
        val distance = g.waveStart + (g.waveEnd - g.waveStart) * travel
        val halfExtent = g.waveExtent * (1f - EXTENT_SHRINK * travel)

        val ratio = (halfExtent / distance).coerceIn(0f, MAX_RATIO)
        val halfAngle = Math.toDegrees(asin(ratio).toDouble()).toFloat()
        val side = waves.sideOf(i)
        val axis = if (side == 0) 180f else 0f

        val topLeft = Offset(g.centerX - distance, g.centerY - distance)
        val size = Size(distance * 2f, distance * 2f)
        val startAngle = axis - halfAngle
        val sweepAngle = halfAngle * 2f
        // Brightness holds most of the way then drops sharply, so the wave is
        // nearly gone by the time it reaches the edge.
        val fade = (1f - progress) * (1f - progress)
        val width = g.dp(WAVE_WIDTH_DP) * (1f - WIDTH_SHRINK * travel)

        drawNeonArc(
            Accent, startAngle, sweepAngle, topLeft, size,
            width * HALO_RATIO, fade * HALO_ALPHA
        )
        drawNeonArc(
            Accent, startAngle, sweepAngle, topLeft, size,
            width, fade * CORE_ALPHA
        )
        drawNeonArc(
            Bone, startAngle, sweepAngle, topLeft, size,
            width * VEIN_RATIO, fade * fade * VEIN_ALPHA
        )
    }
}

private fun DrawScope.drawNeonArc(
    color: Color,
    startAngle: Float,
    sweepAngle: Float,
    topLeft: Offset,
    size: Size,
    width: Float,
    alpha: Float
) {
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = topLeft,
        size = size,
        alpha = alpha,
        style = Stroke(width = width, cap = StrokeCap.Round),
        blendMode = BlendMode.Plus
    )
}

/**
 * The core: a glowing neon ring over a dark well. The well is punched out in
 * true black first, so the circle reads as a hole rather than a sticker. The
 * ring is three passes: halo, stroke, white vein.
 */
fun DrawScope.drawCoreRing(g: HudGeometry, v: KeypadVisuals, lite: Boolean = false) {
    val pulse = v.corePulse
    val radius = g.coreRadius * (1f + pulse * SWELL)
    val ringWidth = g.dp(RING_WIDTH_DP) + g.dp(RING_SWELL_DP) * pulse

    drawCircle(color = Void, radius = radius, center = g.center)

    // Lite mode: one stroke and the dot. Six draw calls become two, and what is
    // lost is depth rather than information.
    if (lite) {
        drawCircle(color = Accent, radius = radius, center = g.center, style = Stroke(ringWidth))
        drawCircle(
            color = Accent,
            radius = g.dp(CORE_DOT_DP) + g.dp(CORE_DOT_SWELL_DP) * pulse,
            center = g.center
        )
        return
    }

    drawOrbitBrackets(g, v.orbitalRotation, pulse, v.wakefulness)

    drawCircle(
        color = Accent,
        radius = radius,
        center = g.center,
        alpha = HALO_ALPHA + pulse * HALO_PULSE_GAIN,
        style = Stroke(ringWidth * HALO_RATIO),
        blendMode = BlendMode.Plus
    )
    drawCircle(
        color = Accent,
        radius = radius,
        center = g.center,
        style = Stroke(ringWidth)
    )
    drawCircle(
        color = Bone,
        radius = radius,
        center = g.center,
        alpha = VEIN_ALPHA + pulse * VEIN_PULSE_GAIN,
        style = Stroke(ringWidth * VEIN_RATIO),
        blendMode = BlendMode.Plus
    )

    drawCircle(
        color = Accent,
        radius = g.dp(CORE_DOT_DP) + g.dp(CORE_DOT_SWELL_DP) * pulse,
        center = g.center,
        blendMode = BlendMode.Plus
    )

    if (pulse > 0f) {
        val expansion = 1f - pulse
        drawCircle(
            color = Accent,
            radius = radius * (1f + expansion * PULSE_REACH),
            center = g.center,
            alpha = pulse * PULSE_ALPHA,
            style = Stroke(g.dp(PULSE_RING_DP)),
            blendMode = BlendMode.Plus
        )
    }
}

/** Sparks: short streaks stretched along their own velocity. */
fun DrawScope.drawSparks(g: HudGeometry, v: KeypadVisuals) {
    val sparks = v.sparks
    for (i in 0 until sparks.capacity) {
        if (!sparks.isAlive(i)) continue

        val fade = 1f - sparks.progress(i)
        val x = g.centerX + sparks.offsetX(i)
        val y = g.centerY + sparks.offsetY(i)
        val tailX = x - sparks.velocityX(i) * STREAK_SECONDS
        val tailY = y - sparks.velocityY(i) * STREAK_SECONDS

        drawLine(
            color = Accent,
            start = Offset(x, y),
            end = Offset(tailX, tailY),
            strokeWidth = g.dp(SPARK_WIDTH_DP) * fade,
            alpha = fade,
            cap = StrokeCap.Round,
            blendMode = BlendMode.Plus
        )
    }
}

/**
 * A ring of arc brackets around the core, spinning faster the harder the player
 * is going. They are the only thing that moves on its own, which is why they
 * fade with [wakefulness]: a pad left on the desk should go still rather than
 * hold a bright pattern against an OLED panel for an hour.
 */
private fun DrawScope.drawOrbitBrackets(
    g: HudGeometry,
    rotation: Float,
    pulse: Float,
    wakefulness: Float
) {
    if (wakefulness <= 0f) return

    val baseRadius = g.coreRadius * ORBIT_RATIO
    val radius = baseRadius * (1f + pulse * ORBIT_SWELL)
    val topLeft = Offset(g.centerX - radius, g.centerY - radius)
    val size = Size(radius * 2f, radius * 2f)
    val startDegrees = Math.toDegrees(rotation.toDouble()).toFloat()

    for (bracket in 0 until ORBIT_BRACKETS) {
        drawArc(
            color = Accent,
            startAngle = startDegrees + bracket * (FULL_TURN / ORBIT_BRACKETS),
            sweepAngle = ORBIT_SWEEP,
            useCenter = false,
            topLeft = topLeft,
            size = size,
            alpha = ORBIT_ALPHA * wakefulness,
            style = Stroke(g.dp(ORBIT_WIDTH_DP), cap = StrokeCap.Round)
        )
    }
}

/** Shares of the quadratic and linear parts in the wave easing. */
private const val EASE_QUADRATIC = 0.6f
private const val EASE_LINEAR = 0.4f

/** An arc pressed too close to the centre would degenerate into a segment. */
private const val MAX_RATIO = 0.995f
private const val EXTENT_SHRINK = 0.36f
private const val WIDTH_SHRINK = 0.45f
private const val WAVE_WIDTH_DP = 12f

/** Neon layers: halo, stroke, white vein. */
private const val HALO_RATIO = 2.6f
private const val HALO_ALPHA = 0.22f
private const val HALO_PULSE_GAIN = 0.2f
private const val CORE_ALPHA = 0.95f
private const val VEIN_RATIO = 0.32f
private const val VEIN_ALPHA = 0.85f
private const val VEIN_PULSE_GAIN = 0.3f

private const val SWELL = 0.06f
private const val RING_WIDTH_DP = 8f
private const val RING_SWELL_DP = 5f
private const val CORE_DOT_DP = 6f
private const val CORE_DOT_SWELL_DP = 14f
private const val PULSE_REACH = 0.45f
private const val PULSE_ALPHA = 0.9f
private const val PULSE_RING_DP = 5f

private const val SPARK_WIDTH_DP = 4f

/** Spark tail: how much of its own path is shown at once. */
private const val STREAK_SECONDS = 0.035f

private const val FULL_TURN = 360f
private const val ORBIT_RATIO = 1.24f
private const val ORBIT_BRACKETS = 3
private const val ORBIT_SWEEP = 48f
private const val ORBIT_ALPHA = 0.45f
private const val ORBIT_SWELL = 0.12f
private const val ORBIT_WIDTH_DP = 4f
