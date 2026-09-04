package sh.aminov.alternate.ui.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sh.aminov.alternate.ui.theme.Accent
import sh.aminov.alternate.ui.theme.AccentDeep
import sh.aminov.alternate.ui.theme.BodyStyle
import sh.aminov.alternate.ui.theme.ButtonSmallStyle
import sh.aminov.alternate.ui.theme.Bone
import sh.aminov.alternate.ui.theme.LabelStyle
import sh.aminov.alternate.ui.theme.Steel
import sh.aminov.alternate.ui.theme.Void
import sh.aminov.alternate.ui.theme.ZoneRed
import androidx.compose.material3.Text

/**
 * A yes-or-no panel over a dimmed screen.
 *
 * Not a Material dialog: the platform one brings its own corner radius,
 * elevation and typography, and would be the only rounded, floating thing in an
 * app built out of notched flat panels. The scrim takes clicks so the list
 * underneath cannot be poked, and dismisses on a tap — the safe answer is
 * always the one that costs nothing.
 *
 * @param confirmLabel the destructive verb, not "OK".
 */
@Composable
fun HudConfirm(
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrim = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Void.copy(alpha = SCRIM_ALPHA))
            .clickable(interactionSource = scrim, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        // A second, inert clickable stops a tap on the panel from reaching the
        // scrim behind it and dismissing the very question being answered.
        val panel = remember { MutableInteractionSource() }
        Column(
            modifier = Modifier
                .widthIn(max = PANEL_MAX_WIDTH)
                .padding(horizontal = 32.dp)
                .clickable(interactionSource = panel, indication = null, onClick = {})
                .hudSurface(Accent)
                .padding(horizontal = 28.dp, vertical = 24.dp)
        ) {
            Text(text = title, style = LabelStyle, color = Accent)
            Text(
                text = message,
                style = BodyStyle,
                color = Bone,
                modifier = Modifier.padding(top = 14.dp, bottom = 24.dp)
            )
            // Both buttons wear the quiet outline: a lit, haloed confirm would
            // be the app inviting the destructive answer. Colour does the work,
            // and this is the one place red means "about to lose something".
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HudButton(
                    label = cancelLabel,
                    enabled = true,
                    onClick = onDismiss,
                    accent = AccentDeep,
                    labelStyle = ButtonSmallStyle,
                    prominent = false,
                    labelColor = Steel,
                    modifier = Modifier.weight(1f).height(BUTTON_HEIGHT)
                )
                HudButton(
                    label = confirmLabel,
                    enabled = true,
                    onClick = onConfirm,
                    accent = ZoneRed,
                    labelStyle = ButtonSmallStyle,
                    prominent = false,
                    labelColor = ZoneRed,
                    modifier = Modifier.weight(1f).height(BUTTON_HEIGHT)
                )
            }
        }
    }
}

private const val SCRIM_ALPHA = 0.86f
private val PANEL_MAX_WIDTH = 520.dp
private val BUTTON_HEIGHT = 54.dp
