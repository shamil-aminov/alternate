package sh.aminov.alternate.ui.screens

import android.media.midi.MidiDeviceInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import sh.aminov.alternate.R
import sh.aminov.alternate.data.models.KeypadConfigState
import sh.aminov.alternate.ui.hud.HudButton
import sh.aminov.alternate.ui.hud.HudNavRow
import sh.aminov.alternate.ui.hud.HudPanel
import sh.aminov.alternate.ui.hud.cockpitBackdrop
import sh.aminov.alternate.ui.theme.Accent
import sh.aminov.alternate.ui.theme.AccentDeep
import sh.aminov.alternate.ui.theme.BodyStyle
import sh.aminov.alternate.ui.theme.Bone
import sh.aminov.alternate.ui.theme.DisplayStyle
import sh.aminov.alternate.ui.theme.LabelStyle
import sh.aminov.alternate.ui.theme.Steel
import sh.aminov.alternate.ui.theme.ZoneRed

/**
 * The connection screen.
 *
 * Landscape on a phone is a strip roughly 960 by 400 dp, so a single centred
 * column would push the button below the fold. Two columns instead: left is
 * identity and what you read once, right is the decision and the action, with
 * the button at the bottom right under the thumb. A tall window gets one
 * scrolling column instead.
 */
@Composable
fun ConfigScreen(
    state: KeypadConfigState,
    onStartClick: () -> Unit,
    onDemoClick: () -> Unit,
    onDeviceSelected: (MidiDeviceInfo?) -> Unit,
    onSettingsClick: () -> Unit,
    onHowItWorksClick: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().cockpitBackdrop()) {
        val wide = maxWidth > maxHeight

        Box(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = SCREEN_PADDING_X, vertical = SCREEN_PADDING_Y)
        ) {
            if (wide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(COLUMN_GAP)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(IDENTITY_WEIGHT)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Wordmark()
                        NavRows(onHowItWorksClick, onSettingsClick)
                    }
                    Column(
                        modifier = Modifier
                            .weight(ACTION_WEIGHT)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(GAP, Alignment.Bottom)
                    ) {
                        ActionColumn(state, onDeviceSelected, onStartClick, onDemoClick)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = CONTENT_MAX_WIDTH)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(GAP)
                ) {
                    Wordmark()
                    ActionColumn(state, onDeviceSelected, onStartClick, onDemoClick)
                    NavRows(onHowItWorksClick, onSettingsClick)
                }
            }
        }
    }
}

/** The device list scrolls on its own so the button can never leave the screen. */
@Composable
private fun ColumnScope.ActionColumn(
    state: KeypadConfigState,
    onDeviceSelected: (MidiDeviceInfo?) -> Unit,
    onStartClick: () -> Unit,
    onDemoClick: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState())
    ) {
        if (state.isMidiSupported) {
            DevicePanel(state, onDeviceSelected)
        } else {
            UnsupportedNotice()
        }
    }

    state.error?.let { message ->
        Text(
            text = message.uppercase(),
            style = LabelStyle,
            color = ZoneRed,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }

    PrimaryAction(state, onStartClick, onDemoClick)
}

/**
 * One button that changes its mind: START with a port to open, DEMO without.
 * Demo opens the same pad with the MIDI send going nowhere, so the app can be
 * tried before anyone finds a cable.
 */
@Composable
private fun PrimaryAction(
    state: KeypadConfigState,
    onStartClick: () -> Unit,
    onDemoClick: () -> Unit
) {
    val canStart = state.isReady && state.isMidiSupported
    HudButton(
        label = when {
            state.isConnecting -> stringResource(R.string.config_connecting)
            canStart -> stringResource(R.string.config_start)
            else -> stringResource(R.string.config_demo)
        },
        enabled = !state.isConnecting,
        onClick = if (canStart) onStartClick else onDemoClick,
        accent = if (canStart) Accent else AccentDeep,
        prominent = canStart,
        labelColor = if (canStart) Accent else Steel,
        modifier = Modifier.fillMaxWidth().height(BUTTON_HEIGHT)
    )
}

@Composable
private fun Wordmark() {
    Column {
        Text(
            text = stringResource(R.string.app_name),
            style = DisplayStyle,
            color = Bone
        )
        Text(
            text = stringResource(R.string.config_tagline),
            style = LabelStyle,
            color = Steel,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

/** Bare rows rather than a panel: these are not a decision. */
@Composable
private fun NavRows(onHowItWorksClick: () -> Unit, onSettingsClick: () -> Unit) {
    Column(Modifier.widthIn(max = NAV_MAX_WIDTH)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(AccentDeep.copy(alpha = 0.4f)))
        HudNavRow(
            label = stringResource(R.string.config_how_it_works),
            description = null,
            onClick = onHowItWorksClick
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(AccentDeep.copy(alpha = 0.4f)))
        HudNavRow(
            label = stringResource(R.string.config_settings),
            description = null,
            onClick = onSettingsClick
        )
    }
}

/** Device selection. */
@Composable
private fun DevicePanel(
    state: KeypadConfigState,
    onDeviceSelected: (MidiDeviceInfo?) -> Unit
) {
    HudPanel(
        title = stringResource(R.string.config_panel_device),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (state.availableDevices.isEmpty()) {
            // No cable is not an error, it is the state everyone starts in.
            Text(
                text = stringResource(R.string.config_no_devices_hint),
                style = BodyStyle,
                color = Steel
            )
        } else {
            state.availableDevices.forEachIndexed { index, device ->
                DeviceRow(
                    device = device,
                    selected = device.id == state.selectedDevice?.id,
                    onClick = { onDeviceSelected(device) },
                    modifier = Modifier.padding(top = if (index == 0) 0.dp else 4.dp)
                )
            }
        }
    }
}

/** One device. The selected one is marked by a bar, not by colour alone. */
@Composable
private fun DeviceRow(
    device: MidiDeviceInfo,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(5.dp)
                .height(26.dp)
                .background(if (selected) Accent else Color.Transparent)
        )
        Text(
            text = device.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
                ?: stringResource(R.string.config_unknown_device),
            style = BodyStyle,
            color = if (selected) Bone else Steel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 14.dp)
        )
    }
}

@Composable
private fun UnsupportedNotice() {
    HudPanel(
        title = stringResource(R.string.config_panel_system),
        accent = ZoneRed,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.config_unsupported),
            style = LabelStyle,
            color = ZoneRed
        )
        Text(
            text = stringResource(R.string.config_unsupported_hint),
            style = BodyStyle,
            color = Steel,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

/** Identity takes less width than the decision does. */
private const val IDENTITY_WEIGHT = 1f
private const val ACTION_WEIGHT = 1.35f

private val SCREEN_PADDING_X = 40.dp
private val SCREEN_PADDING_Y = 24.dp
private val COLUMN_GAP = 48.dp
private val GAP = 16.dp
private val BUTTON_HEIGHT = 76.dp
private val NAV_MAX_WIDTH = 360.dp
private val CONTENT_MAX_WIDTH = 560.dp

