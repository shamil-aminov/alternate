package sh.aminov.alternate.ui.hud

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Building blocks for the instrument panel.
 *
 * Glow is layered strokes with additive blending rather than a blur:
 * `RenderEffect` needs Android 12 and a separate buffer every frame, while an
 * extra stroke is one cheap draw call that works everywhere.
 */

/** A soft halo around a path. */
fun DrawScope.glowPath(
    path: Path,
    color: Color,
    strokeWidth: Float,
    intensity: Float = 1f
) {
    if (intensity > 0f) {
        drawPath(
            path = path,
            color = color,
            alpha = HALO_ALPHA * intensity,
            style = Stroke(strokeWidth * HALO_SPREAD),
            blendMode = BlendMode.Plus
        )
    }
    drawPath(path = path, color = color, style = Stroke(strokeWidth))
}

/**
 * A rectangle with notched corners — the basic shape of the frame. Each notch
 * is optional: asymmetric corners look more deliberate than eight identical
 * bevels.
 *
 * Written into [into] so a frame allocates no path.
 */
fun notchedRect(
    into: Path,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    notch: Float,
    cutTopLeft: Boolean = true,
    cutTopRight: Boolean = false,
    cutBottomRight: Boolean = true,
    cutBottomLeft: Boolean = false
): Path {
    into.rewind()
    if (cutTopLeft) {
        into.moveTo(left, top + notch)
        into.lineTo(left + notch, top)
    } else {
        into.moveTo(left, top)
    }

    if (cutTopRight) {
        into.lineTo(right - notch, top)
        into.lineTo(right, top + notch)
    } else {
        into.lineTo(right, top)
    }

    if (cutBottomRight) {
        into.lineTo(right, bottom - notch)
        into.lineTo(right - notch, bottom)
    } else {
        into.lineTo(right, bottom)
    }

    if (cutBottomLeft) {
        into.lineTo(left + notch, bottom)
        into.lineTo(left, bottom - notch)
    } else {
        into.lineTo(left, bottom)
    }

    into.close()
    return into
}

private const val HALO_SPREAD = 3.2f
private const val HALO_ALPHA = 0.2f
