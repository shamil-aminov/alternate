package sh.aminov.alternate.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sh.aminov.alternate.data.models.AppSettings
import sh.aminov.alternate.keypad.AlternateTuning

/**
 * Persisted settings.
 *
 * `SharedPreferences` rather than DataStore, deliberately: the first read has
 * to finish before the first screen draws, and a flow arriving a frame later
 * would open the pad on defaults and then visibly correct itself. The file is
 * loaded once, synchronously; every later read is a field access. Writes go
 * through `apply()`, since nothing here needs the disk before the next frame.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    /** Current value without collecting the flow. */
    val current: AppSettings get() = _settings.value

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        save(next)
    }

    /**
     * Resets everything the player can change, but not whether they have seen
     * the walkthrough: that would be a reset of the wrong thing.
     */
    fun resetToDefaults() {
        val defaults = AppSettings(hasSeenOnboarding = _settings.value.hasSeenOnboarding)
        _settings.value = defaults
        save(defaults)
    }

    private fun load(): AppSettings {
        val defaults = AppSettings()
        val tuning = AlternateTuning(
            notePrimary = prefs.getInt(KEY_NOTE_PRIMARY, defaults.tuning.notePrimary)
                .coerceIn(AlternateTuning.MIN_NOTE, AlternateTuning.MAX_NOTE),
            noteAlternate = prefs.getInt(KEY_NOTE_ALTERNATE, defaults.tuning.noteAlternate)
                .coerceIn(AlternateTuning.MIN_NOTE, AlternateTuning.MAX_NOTE),
            debounceMs = prefs.getLong(KEY_DEBOUNCE, defaults.tuning.debounceMs)
                .coerceIn(AlternateTuning.MIN_DEBOUNCE_MS, AlternateTuning.MAX_DEBOUNCE_MS),
            chainWindowMs = prefs.getLong(KEY_CHAIN_WINDOW, defaults.tuning.chainWindowMs)
                .coerceIn(
                    AlternateTuning.MIN_CHAIN_WINDOW_MS,
                    AlternateTuning.MAX_CHAIN_WINDOW_MS
                )
        )
        return AppSettings(
            tuning = tuning,
            keepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, defaults.keepScreenOn),
            hideSystemBars = prefs.getBoolean(KEY_HIDE_BARS, defaults.hideSystemBars),
            liteGraphics = prefs.getBoolean(KEY_LITE_GRAPHICS, defaults.liteGraphics),
            autoDoNotDisturb = prefs.getBoolean(KEY_AUTO_DND, defaults.autoDoNotDisturb),
            showDiagnostics = prefs.getBoolean(KEY_DIAGNOSTICS, defaults.showDiagnostics),
            hasSeenOnboarding = prefs.getBoolean(KEY_ONBOARDED, defaults.hasSeenOnboarding)
        )
    }

    private fun save(settings: AppSettings) {
        prefs.edit {
            putInt(KEY_NOTE_PRIMARY, settings.tuning.notePrimary)
            putInt(KEY_NOTE_ALTERNATE, settings.tuning.noteAlternate)
            putLong(KEY_DEBOUNCE, settings.tuning.debounceMs)
            putLong(KEY_CHAIN_WINDOW, settings.tuning.chainWindowMs)
            putBoolean(KEY_KEEP_SCREEN_ON, settings.keepScreenOn)
            putBoolean(KEY_HIDE_BARS, settings.hideSystemBars)
            putBoolean(KEY_LITE_GRAPHICS, settings.liteGraphics)
            putBoolean(KEY_AUTO_DND, settings.autoDoNotDisturb)
            putBoolean(KEY_DIAGNOSTICS, settings.showDiagnostics)
            putBoolean(KEY_ONBOARDED, settings.hasSeenOnboarding)
        }
    }

    private companion object {
        const val FILE_NAME = "alternate_settings"

        const val KEY_NOTE_PRIMARY = "note_primary"
        const val KEY_NOTE_ALTERNATE = "note_alternate"
        const val KEY_DEBOUNCE = "debounce_ms"
        const val KEY_CHAIN_WINDOW = "chain_window_ms"
        const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        const val KEY_HIDE_BARS = "hide_system_bars"
        const val KEY_LITE_GRAPHICS = "lite_graphics"
        const val KEY_AUTO_DND = "auto_dnd"
        const val KEY_DIAGNOSTICS = "diagnostics"
        const val KEY_ONBOARDED = "onboarded"
    }
}
