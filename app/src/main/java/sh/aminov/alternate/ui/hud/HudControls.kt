package sh.aminov.alternate.ui.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sh.aminov.alternate.ui.theme.Accent
import sh.aminov.alternate.ui.theme.AccentDeep
import sh.aminov.alternate.ui.theme.BodyStyle
import sh.aminov.alternate.ui.theme.Bone
import sh.aminov.alternate.ui.theme.CaptionStyle
import sh.aminov.alternate.ui.theme.ChevronStyle
import sh.aminov.alternate.ui.theme.LabelStyle
import sh.aminov.alternate.ui.theme.ReadoutSmallStyle
import sh.aminov.alternate.ui.theme.Steel

/**
 * Rows for the settings list: notched edges, no Material chrome.
 *
 * Steppers rather than sliders — every setting here is a number a player wants
 * to land on exactly, and a slider on a phone cannot hit one. Every row ends in
 * a control area of the same width, so the minus buttons line up down the page
 * and with the switches beside them.
 */

/** The way back. No title beside it, so nothing to misalign against. */
@Composable
fun HudTopBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(BACK_TARGET)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onBack
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "‹", style = ChevronStyle, color = Accent)
    }
}

/** The name of a group of rows, with the rule that opens it. */
@Composable
fun HudSectionHeader(text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text(
            text = text,
            style = LabelStyle,
            color = Accent,
            modifier = Modifier.padding(top = SECTION_TOP, bottom = 10.dp)
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(AccentDeep.copy(alpha = 0.55f)))
    }
}

/** An on/off row. The whole row is the target, not just the switch. */
@Composable
fun HudToggle(
    label: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    SettingRow(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = { onCheckedChange(!checked) }
        ),
        label = label,
        description = description,
        labelColor = if (enabled) Bone else Steel
    ) {
        Switch(checked = checked, enabled = enabled)
    }
}

/** [valueText] is passed in so a setting can show its own units. */
@Composable
fun HudStepper(
    label: String,
    valueText: String,
    description: String?,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    canDecrease: Boolean,
    canIncrease: Boolean,
    modifier: Modifier = Modifier
) {
    SettingRow(modifier = modifier, label = label, description = description) {
        StepButton(symbol = "−", enabled = canDecrease, onClick = onDecrease)
        Text(
            text = valueText,
            style = ReadoutSmallStyle,
            color = Accent,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.width(VALUE_WIDTH)
        )
        StepButton(symbol = "+", enabled = canIncrease, onClick = onIncrease)
    }
}

/** A row that leads somewhere. The chevron says so without a word. */
@Composable
fun HudNavRow(
    label: String,
    description: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    SettingRow(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        ),
        label = label,
        description = description,
        // A chevron needs no stepper-sized reservation, and claiming one would
        // squeeze the label where these rows appear in a narrow column.
        controlWidth = CHEVRON_WIDTH
    ) {
        Text(text = "›", style = ReadoutSmallStyle, color = Accent)
    }
}

/** Text on the left, a control area of one fixed width on the right. */
@Composable
private fun SettingRow(
    label: String,
    description: String?,
    modifier: Modifier = Modifier,
    labelColor: Color = Bone,
    controlWidth: Dp = CONTROL_WIDTH,
    control: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = ROW_PADDING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = label, style = BodyStyle, color = labelColor)
            if (description != null) {
                Text(
                    text = description,
                    style = CaptionStyle,
                    color = Steel,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
        Spacer(Modifier.width(GAP))
        Row(
            modifier = Modifier.width(controlWidth),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            control()
        }
    }
}

/**
 * A track with a block at one end. Drawn rather than animated between
 * positions — at this size a slide reads as a stutter. The track shows in both
 * states: without it, "off" is a grey square floating in black.
 */
@Composable
private fun Switch(checked: Boolean, enabled: Boolean) {
    val active = if (enabled) Accent else AccentDeep
    Row(
        modifier = Modifier
            .width(SWITCH_WIDTH)
            .height(SWITCH_HEIGHT)
            .background(
                if (checked) active.copy(alpha = TRACK_ON_ALPHA)
                else Steel.copy(alpha = TRACK_OFF_ALPHA)
            ),
        horizontalArrangement = if (checked) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = SWITCH_KNOB, height = SWITCH_HEIGHT)
                .background(if (checked) active else Steel.copy(alpha = KNOB_OFF_ALPHA))
        )
    }
}

@Composable
private fun StepButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val tint = if (enabled) Accent else AccentDeep.copy(alpha = 0.4f)
    Box(
        modifier = Modifier
            .size(STEP_BUTTON)
            .background(tint.copy(alpha = 0.12f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text = symbol, style = ReadoutSmallStyle, color = tint)
    }
}

/** A thin rule between rows. */
@Composable
fun HudDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AccentDeep.copy(alpha = 0.25f))
    )
}

private val ROW_PADDING = 14.dp
private val SECTION_TOP = 24.dp
private val GAP = 16.dp

/** Every row's control area is this wide, so the columns line up. */
private val CONTROL_WIDTH = 220.dp

private val VALUE_WIDTH = 116.dp
private val SWITCH_WIDTH = 52.dp
private val SWITCH_HEIGHT = 28.dp
private val SWITCH_KNOB = 22.dp
private val STEP_BUTTON = 44.dp
private val BACK_TARGET = 48.dp
private val CHEVRON_WIDTH = 24.dp

private const val TRACK_ON_ALPHA = 0.22f
private const val TRACK_OFF_ALPHA = 0.12f
private const val KNOB_OFF_ALPHA = 0.55f
