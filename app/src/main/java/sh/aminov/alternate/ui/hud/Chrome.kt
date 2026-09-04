package sh.aminov.alternate.ui.hud

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import sh.aminov.alternate.keypad.BpmScale
import sh.aminov.alternate.keypad.KeypadVisuals
import sh.aminov.alternate.ui.theme.Accent
import sh.aminov.alternate.ui.theme.AccentDeep
import sh.aminov.alternate.ui.theme.Bone
import sh.aminov.alternate.ui.theme.Void

/** The field itself. */
fun DrawScope.drawBackdrop() {
    drawRect(color = Void)
}

/** The vignette that pulls the eye back to the centre. */
fun DrawScope.drawCockpitFrame(backdrop: CockpitBackdrop) {
    drawRect(brush = backdrop.vignette)
}

/** Peripheral feedback: it registers while the eyes stay on the core. */
fun DrawScope.drawEdgeFlash(g: HudGeometry, v: KeypadVisuals, backdrop: CockpitBackdrop) {
    val height = g.height
    val left = v.edgeFlash[0]
    if (left > 0f) {
        drawRect(
            brush = backdrop.edgeLeft,
            topLeft = Offset.Zero,
            size = Size(g.edgeFlashWidth, height),
            alpha = left * EDGE_ALPHA,
            blendMode = BlendMode.Plus
        )
    }
    val right = v.edgeFlash[1]
    if (right > 0f) {
        drawRect(
            brush = backdrop.edgeRight,
            topLeft = Offset(g.width - g.edgeFlashWidth, 0f),
            size = Size(g.edgeFlashWidth, height),
            alpha = right * EDGE_ALPHA,
            blendMode = BlendMode.Plus
        )
    }
}

/**
 * The tempo gauge: a column of outlined blocks that light up.
 *
 * One hue, from the deep blue to the accent. Height carries the reading and
 * the shading lets a block be watched arriving; there is no second voice
 * saying the same thing — no ticks alongside, no peak line, no zone colours.
 *
 * Block geometry is worked out once at construction, so the draw phase only
 * picks what to paint and how brightly and a frame allocates nothing.
 */
class GaugePainter(private val g: HudGeometry) {

    private val cellOutlineWidth = g.dp(CELL_OUTLINE_DP)
    private val gap = g.dp(SEGMENT_GAP_DP)

    private val innerHeight = (g.gaugeBottom - g.gaugeTop).coerceAtLeast(0f)
    private val segmentHeight =
        ((innerHeight - gap * (SEGMENTS - 1)) / SEGMENTS).coerceAtLeast(0f)

    private val segmentPath = Path()

    /** The unlit scale. Static, so it belongs in the chrome layer. */
    fun drawBed(scope: DrawScope) {
        if (segmentHeight <= 0f) return
        for (i in 0 until SEGMENTS) {
            buildSegment(i)
            scope.drawPath(
                path = segmentPath,
                color = Accent,
                alpha = CELL_BED_ALPHA,
                style = Stroke(cellOutlineWidth)
            )
        }
    }

    /**
     * The lit blocks, each at its own spring level. The level runs past 1 as a
     * block comes on; that overshoot is spent on the flare rather than on size
     * or position, so a block pops without anything moving.
     */
    fun drawFill(scope: DrawScope, visuals: KeypadVisuals, lite: Boolean) {
        if (segmentHeight <= 0f) return

        for (i in 0 until SEGMENTS) {
            val level = visuals.gaugeCell(i)
            if (level <= 0f) continue

            val settled = level.coerceAtMost(1f)
            val overshoot = (level - 1f).coerceAtLeast(0f)
            val colour = lerp(
                lerp(AccentDeep, Accent, settled),
                Bone,
                (overshoot * FLARE_GAIN).coerceAtMost(1f)
            )

            buildSegment(i)
            if (!lite) {
                scope.drawPath(
                    path = segmentPath,
                    color = colour,
                    alpha = (HALO_ALPHA * settled + overshoot * FLARE_GAIN).coerceAtMost(1f),
                    blendMode = BlendMode.Plus
                )
            }
            scope.drawPath(
                path = segmentPath,
                color = colour,
                alpha = FILL_ALPHA_FLOOR + settled * (1f - FILL_ALPHA_FLOOR)
            )
        }
    }

    /**
     * Block [index] from the bottom. Every edge comes from the bar bounds, not
     * from the fill level — deriving the top edge from the level is what once
     * flipped a nearly empty gauge inside out and drew it downwards.
     */
    private fun buildSegment(index: Int) {
        val bottom = g.gaugeBottom - index * (segmentHeight + gap)
        val top = bottom - segmentHeight
        segmentPath.rewind()
        segmentPath.moveTo(g.gaugeRight, bottom)
        segmentPath.lineTo(g.gaugeRight - widthAt(bottom), bottom)
        segmentPath.lineTo(g.gaugeRight - widthAt(top), top)
        segmentPath.lineTo(g.gaugeRight, top)
        segmentPath.close()
    }

    /** The bar leans outward as it climbs, so higher is wider as well. */
    private fun widthAt(y: Float): Float {
        val t = ((g.gaugeBottom - y) / g.gaugeHeight).coerceIn(0f, 1f)
        return g.gaugeWidth * (1f - t) + g.gaugeWidth * TOP_WIDTH_RATIO * t
    }

    private companion object {
        val SEGMENTS = BpmScale.STEPS
        const val SEGMENT_GAP_DP = 4f
        const val TOP_WIDTH_RATIO = 1.45f
        const val CELL_BED_ALPHA = 0.20f
        const val CELL_OUTLINE_DP = 1.5f
        const val FLARE_GAIN = 2.5f
        const val HALO_ALPHA = 0.4f

        /** A partly lit block never sits fully transparent. */
        const val FILL_ALPHA_FLOOR = 0.08f
    }
}

/** Brushes that depend only on screen size, cached per size by `drawWithCache`. */
class CockpitBackdrop(g: HudGeometry) {

    val vignette: Brush = Brush.radialGradient(
        colors = listOf(Color.Transparent, Void.copy(alpha = VIGNETTE_ALPHA)),
        center = g.center,
        radius = maxOf(g.width, g.height) * VIGNETTE_RADIUS_RATIO
    )

    val edgeLeft: Brush = Brush.horizontalGradient(
        colors = listOf(Accent, Color.Transparent),
        startX = 0f,
        endX = g.edgeFlashWidth
    )

    val edgeRight: Brush = Brush.horizontalGradient(
        colors = listOf(Color.Transparent, Accent),
        startX = g.width - g.edgeFlashWidth,
        endX = g.width
    )

    private companion object {
        const val VIGNETTE_ALPHA = 0.75f
        const val VIGNETTE_RADIUS_RATIO = 0.6f
    }
}

private const val EDGE_ALPHA = 0.5f
