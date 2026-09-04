package sh.aminov.alternate.data.models

import android.media.midi.MidiDeviceInfo

/**
 * State of the connection screen.
 *
 * @param isConnecting a port is being opened right now.
 * @param error last connection error; shown to the user rather than only
 *   written to the log.
 */
data class KeypadConfigState(
    val selectedDevice: MidiDeviceInfo? = null,
    val availableDevices: List<MidiDeviceInfo> = emptyList(),
    val isMidiSupported: Boolean = true,
    val isConnecting: Boolean = false,
    val error: String? = null
) {
    val isReady: Boolean get() = selectedDevice != null && !isConnecting
}
