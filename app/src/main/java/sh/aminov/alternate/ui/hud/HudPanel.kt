package sh.aminov.alternate.ui.hud

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sh.aminov.alternate.ui.theme.Accent
import sh.aminov.alternate.ui.theme.AccentDeep
import sh.aminov.alternate.ui.theme.ButtonStyle
import sh.aminov.alternate.ui.theme.Ink
import sh.aminov.alternate.ui.theme.LabelStyle

/** A notched panel with a small caps title inside it. */
@Composable
fun HudPanel(
    title: String,
    modifier: Modifier = Modifier,
    accent: Color = Accent,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .hudSurface(accent)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Text(
            text = title,
            style = LabelStyle.copy(fontSize = 12.sp, letterSpacing = 1.sp),
            color = accent.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}

/**
 * Large notched button, in three looks.
 *
 * Disabled is thin and dark. Primary is thick and haloed. Secondary — with
 * [prominent] off — keeps the outline but drops the halo: a wide glow in a
 * desaturated colour reads as a blurry shadow, not as a quieter button.
 */
@Composable
fun HudButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Accent,
    labelStyle: TextStyle = ButtonStyle,
    prominent: Boolean = true,
    labelColor: Color = accent
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val edge = if (enabled) accent else AccentDeep

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .drawWithCache {
                val path = Path()
                val notch = 20.dp.toPx()
                val edgeWidth = when {
                    !enabled -> 2.5.dp.toPx()
                    prominent -> 5.dp.toPx()
                    else -> 3.dp.toPx()
                }
                val fillAlpha = when {
                    !enabled -> 0f
                    pressed -> 0.42f
                    prominent -> 0.16f
                    else -> 0.07f
                }
                onDrawBehind {
                    notchedRect(
                        into = path,
                        left = 0f,
                        top = 0f,
                        right = size.width,
                        bottom = size.height,
                        notch = notch
                    )
                    drawPath(path = path, color = Ink)
                    drawPath(path = path, color = accent, alpha = fillAlpha)
                    glowPath(
                        path = path,
                        color = edge,
                        strokeWidth = edgeWidth,
                        intensity = if (enabled && prominent) 1.4f else 0f
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = labelStyle,
            color = if (enabled) labelColor else AccentDeep
        )
    }
}

/** Panel surface: notched fill and border. */
fun Modifier.hudSurface(accent: Color): Modifier = this.drawWithCache {
    val path = Path()
    val notch = 18.dp.toPx()
    val stroke = 3.dp.toPx()
    onDrawBehind {
        notchedRect(
            into = path,
            left = 0f,
            top = 0f,
            right = size.width,
            bottom = size.height,
            notch = notch
        )
        drawPath(path = path, color = Ink, alpha = 0.92f)
        drawPath(path = path, color = accent, alpha = 0.55f, style = Stroke(stroke))
    }
}

/** The shared background for every screen that is not the pad. */
fun Modifier.cockpitBackdrop(): Modifier = this.drawWithCache {
    val geometry = HudGeometry(size, density)
    val backdrop = CockpitBackdrop(geometry)
    onDrawBehind {
        drawBackdrop()
        drawRect(brush = backdrop.vignette)
    }
}
