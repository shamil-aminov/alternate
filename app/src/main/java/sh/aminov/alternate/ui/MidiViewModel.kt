package sh.aminov.alternate.ui

import android.app.Application
import android.media.midi.MidiDeviceInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sh.aminov.alternate.R
import sh.aminov.alternate.data.DoNotDisturbController
import sh.aminov.alternate.data.MidiOpenResult
import sh.aminov.alternate.data.MidiRepository
import sh.aminov.alternate.data.SettingsRepository
import sh.aminov.alternate.data.models.AppSettings
import sh.aminov.alternate.data.models.KeypadConfigState

class MidiViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MidiRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val doNotDisturb = DoNotDisturbController(application)

    val settings: StateFlow<AppSettings> = settingsRepository.settings

    private val _uiState = MutableStateFlow(
        KeypadConfigState(
            isMidiSupported = repository.isMidiSupported(),
            availableDevices = repository.getDevices()
        )
    )
    val uiState: StateFlow<KeypadConfigState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeDevices().collect(::onDevicesChanged)
        }
    }

    /**
     * Keeps the selection honest: a vanished device is dropped, and a new one
     * is chosen automatically — almost always the built-in Android port.
     */
    private fun onDevicesChanged(devices: List<MidiDeviceInfo>) {
        _uiState.update { state ->
            val stillPresent = state.selectedDevice?.takeIf { selected ->
                devices.any { it.id == selected.id }
            }
            val selected = stillPresent ?: devices.preferredDefault()
            state.copy(availableDevices = devices, selectedDevice = selected)
        }
    }

    private fun List<MidiDeviceInfo>.preferredDefault(): MidiDeviceInfo? =
        firstOrNull { it.properties.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER) == ANDROID_VENDOR }
            ?: firstOrNull()

    fun selectDevice(device: MidiDeviceInfo?) {
        _uiState.update { it.copy(selectedDevice = device, error = null) }
    }

    /** Opens the port of the selected device; [onReady] runs on success. */
    fun startKeypad(onReady: () -> Unit) {
        val device = _uiState.value.selectedDevice
        if (device == null) {
            _uiState.update {
                it.copy(error = getApplication<Application>().getString(R.string.error_select_device))
            }
            return
        }

        _uiState.update { it.copy(isConnecting = true, error = null) }
        repository.openDevice(device) { result ->
            _uiState.update {
                it.copy(
                    isConnecting = false,
                    error = (result as? MidiOpenResult.Failure)?.reason
                )
            }
            if (result is MidiOpenResult.Success) onReady()
        }
    }

    /**
     * Sends a note. Called from the touch thread, so it only forwards
     * configuration — no state is touched here.
     */
    fun sendNote(note: Int, isPressed: Boolean) {
        val tuning = settingsRepository.current.tuning
        repository.sendNote(
            note = note,
            isPressed = isPressed,
            channel = tuning.midiChannel,
            velocity = tuning.velocity
        )
    }

    /** Insurance against a stuck note when the app loses focus. */
    fun panic() {
        repository.allNotesOff(settingsRepository.current.tuning.midiChannel)
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settingsRepository.update(transform)
    }

    /** Remembers that the walkthrough is done, so it opens once and no more. */
    fun markOnboardingSeen() {
        settingsRepository.update { it.copy(hasSeenOnboarding = true) }
    }

    fun resetSettings() {
        settingsRepository.resetToDefaults()
    }

    // --- Do Not Disturb ---

    private val _isDoNotDisturbGranted = MutableStateFlow(doNotDisturb.isGranted)

    /**
     * Whether Android has granted notification policy access. State rather than
     * a plain call, because the grant happens on a system screen: without
     * something to observe, the toggle would still read "not granted" on return.
     */
    val isDoNotDisturbGranted: StateFlow<Boolean> = _isDoNotDisturbGranted.asStateFlow()

    /** Called when the activity resumes — the grant may have changed. */
    fun refreshDoNotDisturbAccess() {
        _isDoNotDisturbGranted.value = doNotDisturb.isGranted
    }

    fun doNotDisturbSettingsIntent() = doNotDisturb.accessSettingsIntent()

    /** Called on entering the pad. Silent no-op without the grant. */
    fun engageDoNotDisturb() {
        if (settingsRepository.current.autoDoNotDisturb) doNotDisturb.engage()
    }

    /** Called on leaving the pad, and on the way out of the app. */
    fun releaseDoNotDisturb() {
        doNotDisturb.release()
    }

    override fun onCleared() {
        panic()
        doNotDisturb.release()
        repository.closeCurrentDevice()
    }

    private companion object {
        const val ANDROID_VENDOR = "Android"
    }
}
