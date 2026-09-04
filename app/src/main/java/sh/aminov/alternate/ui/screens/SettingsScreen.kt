package sh.aminov.alternate.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import sh.aminov.alternate.R
import sh.aminov.alternate.data.models.AppSettings
import sh.aminov.alternate.keypad.AlternateTuning
import sh.aminov.alternate.keypad.MidiNotes
import sh.aminov.alternate.ui.hud.HudConfirm
import sh.aminov.alternate.ui.hud.HudDivider
import sh.aminov.alternate.ui.hud.HudNavRow
import sh.aminov.alternate.ui.hud.HudSectionHeader
import sh.aminov.alternate.ui.hud.HudStepper
import sh.aminov.alternate.ui.hud.HudToggle
import sh.aminov.alternate.ui.hud.HudTopBar
import sh.aminov.alternate.ui.hud.cockpitBackdrop
import sh.aminov.alternate.ui.theme.AccentDeep
import sh.aminov.alternate.ui.theme.Steel

/**
 * Settings, as one list.
 *
 * A single column of rows with section headings, not a stack of panels: a
 * border, a title bar and an inset per group is three kinds of chrome to say
 * what a line of small caps says on its own. Groups are by what a setting
 * affects, so "why is the game not reacting" leads to MIDI rather than to a
 * flat list of eleven rows.
 *
 * @param isDoNotDisturbGranted without the grant the toggle cannot do anything.
 * @param onRequestDoNotDisturbAccess opens the system grant screen.
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    isDoNotDisturbGranted: Boolean,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit,
    onRequestDoNotDisturbAccess: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    val tuning = settings.tuning
    val confirmingReset = remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().cockpitBackdrop()) {
        // Laid out like the instructions screen: the chevron gets its own row
        // at the top and keeps the corner to itself, while the list is centred
        // below it and capped at a width that keeps a label and its control
        // inside one glance.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = SCREEN_PADDING, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HudTopBar(onBack = onBack, modifier = Modifier.align(Alignment.Start))

            Column(
                modifier = Modifier
                    .widthIn(max = CONTENT_MAX_WIDTH)
                    .verticalScroll(rememberScrollState())
            ) {
                EngineSection(tuning, onSettingsChange)
                MidiSection(tuning, onSettingsChange)
                ScreenSection(settings, onSettingsChange)
                NotificationsSection(
                    settings = settings,
                    isGranted = isDoNotDisturbGranted,
                    onSettingsChange = onSettingsChange,
                    onRequestAccess = onRequestDoNotDisturbAccess
                )
                DiagnosticsSection(settings, onSettingsChange)

                // A row, not a button parked at the end: it undoes everything
                // above it, so it asks first.
                HudSectionHeader(stringResource(R.string.settings_panel_reset))
                HudNavRow(
                    label = stringResource(R.string.settings_reset),
                    description = stringResource(R.string.settings_reset_hint),
                    onClick = { confirmingReset.value = true }
                )
                Spacer(Modifier.height(24.dp))
            }
        }

        if (confirmingReset.value) {
            HudConfirm(
                title = stringResource(R.string.settings_reset_confirm_title),
                message = stringResource(R.string.settings_reset_confirm_body),
                confirmLabel = stringResource(R.string.action_reset),
                cancelLabel = stringResource(R.string.action_cancel),
                onConfirm = {
                    confirmingReset.value = false
                    onReset()
                },
                onDismiss = { confirmingReset.value = false }
            )
        }
    }
}

@Composable
private fun ColumnScope.EngineSection(
    tuning: AlternateTuning,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit
) {
    HudSectionHeader(stringResource(R.string.settings_panel_engine))
    HudStepper(
        label = stringResource(R.string.settings_debounce),
        valueText = stringResource(R.string.settings_unit_ms, tuning.debounceMs.toInt()),
        description = stringResource(R.string.settings_debounce_hint),
        onDecrease = {
            onSettingsChange { it.withTuning { t -> t.copy(debounceMs = t.debounceMs - 1) } }
        },
        onIncrease = {
            onSettingsChange { it.withTuning { t -> t.copy(debounceMs = t.debounceMs + 1) } }
        },
        canDecrease = tuning.debounceMs > AlternateTuning.MIN_DEBOUNCE_MS,
        canIncrease = tuning.debounceMs < AlternateTuning.MAX_DEBOUNCE_MS
    )
    HudDivider()
    HudStepper(
        label = stringResource(R.string.settings_chain_window),
        valueText = stringResource(R.string.settings_unit_ms, tuning.chainWindowMs.toInt()),
        description = stringResource(R.string.settings_chain_window_hint),
        onDecrease = {
            onSettingsChange {
                it.withTuning { t -> t.copy(chainWindowMs = t.chainWindowMs - CHAIN_WINDOW_STEP_MS) }
            }
        },
        onIncrease = {
            onSettingsChange {
                it.withTuning { t -> t.copy(chainWindowMs = t.chainWindowMs + CHAIN_WINDOW_STEP_MS) }
            }
        },
        canDecrease = tuning.chainWindowMs > AlternateTuning.MIN_CHAIN_WINDOW_MS,
        canIncrease = tuning.chainWindowMs < AlternateTuning.MAX_CHAIN_WINDOW_MS
    )
}

@Composable
private fun ColumnScope.MidiSection(
    tuning: AlternateTuning,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit
) {
    HudSectionHeader(stringResource(R.string.settings_panel_midi))
    HudStepper(
        label = stringResource(R.string.settings_note_left),
        valueText = MidiNotes.describe(tuning.notePrimary),
        description = stringResource(R.string.settings_note_hint),
        onDecrease = {
            onSettingsChange { it.withTuning { t -> t.copy(notePrimary = t.notePrimary - 1) } }
        },
        onIncrease = {
            onSettingsChange { it.withTuning { t -> t.copy(notePrimary = t.notePrimary + 1) } }
        },
        canDecrease = tuning.notePrimary > AlternateTuning.MIN_NOTE,
        canIncrease = tuning.notePrimary < AlternateTuning.MAX_NOTE
    )
    HudDivider()
    HudStepper(
        label = stringResource(R.string.settings_note_right),
        valueText = MidiNotes.describe(tuning.noteAlternate),
        description = null,
        onDecrease = {
            onSettingsChange { it.withTuning { t -> t.copy(noteAlternate = t.noteAlternate - 1) } }
        },
        onIncrease = {
            onSettingsChange { it.withTuning { t -> t.copy(noteAlternate = t.noteAlternate + 1) } }
        },
        canDecrease = tuning.noteAlternate > AlternateTuning.MIN_NOTE,
        canIncrease = tuning.noteAlternate < AlternateTuning.MAX_NOTE
    )
    // Channel and velocity are not offered: osu! binds on pitch and ignores
    // velocity, so both are knobs that can only break the setup.
}

@Composable
private fun ColumnScope.ScreenSection(
    settings: AppSettings,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit
) {
    HudSectionHeader(stringResource(R.string.settings_panel_screen))
    HudToggle(
        label = stringResource(R.string.settings_keep_screen_on),
        description = null,
        checked = settings.keepScreenOn,
        onCheckedChange = { on -> onSettingsChange { it.copy(keepScreenOn = on) } }
    )
    HudDivider()
    HudToggle(
        label = stringResource(R.string.settings_hide_bars),
        description = stringResource(R.string.settings_hide_bars_hint),
        checked = settings.hideSystemBars,
        onCheckedChange = { on -> onSettingsChange { it.copy(hideSystemBars = on) } }
    )
    HudDivider()
    HudToggle(
        label = stringResource(R.string.settings_lite_graphics),
        description = stringResource(R.string.settings_lite_graphics_hint),
        checked = settings.liteGraphics,
        onCheckedChange = { on -> onSettingsChange { it.copy(liteGraphics = on) } }
    )
}

@Composable
private fun ColumnScope.NotificationsSection(
    settings: AppSettings,
    isGranted: Boolean,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit,
    onRequestAccess: () -> Unit
) {
    HudSectionHeader(stringResource(R.string.settings_panel_notifications))
    HudToggle(
        label = stringResource(R.string.settings_auto_dnd),
        description = stringResource(R.string.settings_auto_dnd_hint),
        checked = settings.autoDoNotDisturb && isGranted,
        enabled = isGranted,
        onCheckedChange = { on -> onSettingsChange { it.copy(autoDoNotDisturb = on) } }
    )
    if (!isGranted) {
        HudDivider()
        HudNavRow(
            label = stringResource(R.string.settings_dnd_grant),
            description = stringResource(R.string.settings_dnd_grant_hint),
            onClick = onRequestAccess
        )
    }
}

@Composable
private fun ColumnScope.DiagnosticsSection(
    settings: AppSettings,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit
) {
    HudSectionHeader(stringResource(R.string.settings_panel_diagnostics))
    HudToggle(
        label = stringResource(R.string.settings_show_diagnostics),
        description = stringResource(R.string.settings_show_diagnostics_hint),
        checked = settings.showDiagnostics,
        onCheckedChange = { on -> onSettingsChange { it.copy(showDiagnostics = on) } }
    )
}

/** Applies a change to the tuning and clamps it, so no stepper repeats a limit. */
private fun AppSettings.withTuning(transform: (AlternateTuning) -> AlternateTuning): AppSettings {
    val next = transform(tuning)
    return copy(
        tuning = next.copy(
            notePrimary = next.notePrimary
                .coerceIn(AlternateTuning.MIN_NOTE, AlternateTuning.MAX_NOTE),
            noteAlternate = next.noteAlternate
                .coerceIn(AlternateTuning.MIN_NOTE, AlternateTuning.MAX_NOTE),
            debounceMs = next.debounceMs
                .coerceIn(AlternateTuning.MIN_DEBOUNCE_MS, AlternateTuning.MAX_DEBOUNCE_MS),
            chainWindowMs = next.chainWindowMs
                .coerceIn(
                    AlternateTuning.MIN_CHAIN_WINDOW_MS,
                    AlternateTuning.MAX_CHAIN_WINDOW_MS
                )
        )
    )
}

private const val CHAIN_WINDOW_STEP_MS = 100L

private val SCREEN_PADDING = 40.dp
private val BUTTON_HEIGHT = 68.dp
/** Capped well short of the screen so controls stay near their labels. */
private val CONTENT_MAX_WIDTH = 660.dp
