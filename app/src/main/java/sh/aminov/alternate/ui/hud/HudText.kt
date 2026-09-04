package sh.aminov.alternate.ui.hud

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Text with a halo: a blurred shadow at zero offset, not an outline. An
 * outline cut the digits in half; the halo gives the same depth softly.
 *
 * @param lite drop the shadow. It is the most expensive thing per pixel on the
 *   pad, and the readouts repaint whenever their value changes.
 */
@Composable
fun NeonText(
    text: String,
    style: TextStyle,
    color: Color,
    glow: Color,
    modifier: Modifier = Modifier,
    glowRadius: Dp = 16.dp,
    lite: Boolean = false
) {
    val radiusPx = with(LocalDensity.current) { glowRadius.toPx() }
    val glowing = remember(style, glow, radiusPx, lite) {
        if (lite) style else {
            style.copy(shadow = Shadow(color = glow, offset = Offset.Zero, blurRadius = radiusPx))
        }
    }
    Text(text = text, style = glowing, color = color, modifier = modifier)
}
