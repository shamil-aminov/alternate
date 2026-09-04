package sh.aminov.alternate.data

import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import sh.aminov.alternate.R

/** Outcome of opening a device, in a shape fit to show the user. */
sealed interface MidiOpenResult {
    data object Success : MidiOpenResult
    data class Failure(val reason: String) : MidiOpenResult
}

/**
 * Access to system MIDI: finding devices, opening a port, sending notes.
 *
 * [sendNote] is on the hot path, called straight from the touch handler, so
 * it allocates nothing, logs nothing and locks nothing, and sends synchronously
 * on the touch thread: handing the write to another thread would add
 * scheduling to the latency for no gain.
 *
 * Two buffers, because [allNotesOff] runs on the main thread while [sendNote]
 * runs on the touch thread; sharing one would let a panic message interleave
 * with a note and put three bytes of nonsense on the wire.
 */
class MidiRepository(private val context: Context) {

    private val midiManager: MidiManager? =
        context.getSystemService(Context.MIDI_SERVICE) as? MidiManager

    /** Written on the main thread, read on the touch thread. */
    @Volatile
    private var inputPort: MidiInputPort? = null

    private var openedDevice: MidiDevice? = null

    /** Note on/off buffer. Touch thread only. */
    private val noteBuffer = ByteArray(MESSAGE_SIZE)

    /** Panic buffer. Main thread only. */
    private val panicBuffer = ByteArray(MESSAGE_SIZE)

    fun isMidiSupported(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI)

    /**
     * The deprecated `devices` and `registerDeviceCallback` are deliberate:
     * their replacements landed in Android 13 and this app supports Android 7.
     */
    @Suppress("DEPRECATION")
    fun getDevices(): List<MidiDeviceInfo> = midiManager?.devices?.toList() ?: emptyList()

    /** Device list that keeps up with USB being plugged and unplugged. */
    @Suppress("DEPRECATION")
    fun observeDevices(): Flow<List<MidiDeviceInfo>> = callbackFlow {
        val manager = midiManager
        if (manager == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val callback = object : MidiManager.DeviceCallback() {
            override fun onDeviceAdded(device: MidiDeviceInfo?) {
                trySend(getDevices())
            }

            override fun onDeviceRemoved(device: MidiDeviceInfo?) {
                // The cable may have been ours. Dropping the port now beats
                // sending notes into a handle the system has already revoked.
                if (device != null && device.id == openedDevice?.info?.id) {
                    closeCurrentDevice()
                }
                trySend(getDevices())
            }
        }

        manager.registerDeviceCallback(callback, Handler(Looper.getMainLooper()))
        trySend(getDevices())

        awaitClose { manager.unregisterDeviceCallback(callback) }
    }

    /**
     * Opens the first input port of the device. The result arrives in
     * [onResult] on the main thread.
     */
    fun openDevice(deviceInfo: MidiDeviceInfo, onResult: (MidiOpenResult) -> Unit) {
        closeCurrentDevice()

        val manager = midiManager
        if (manager == null) {
            onResult(failure(R.string.error_midi_unavailable))
            return
        }

        val portInfo = deviceInfo.ports
            .firstOrNull { it.type == MidiDeviceInfo.PortInfo.TYPE_INPUT }
        if (portInfo == null) {
            onResult(failure(R.string.error_no_input_port))
            return
        }

        manager.openDevice(deviceInfo, { device ->
            if (device == null) {
                onResult(failure(R.string.error_open_failed))
            } else {
                openedDevice = device
                val port = runCatching { device.openInputPort(portInfo.portNumber) }.getOrNull()
                if (port == null) {
                    closeCurrentDevice()
                    onResult(failure(R.string.error_port_busy))
                } else {
                    inputPort = port
                    onResult(MidiOpenResult.Success)
                }
            }
        }, Handler(Looper.getMainLooper()))
    }

    /**
     * Sends Note On or Note Off.
     *
     * The port can vanish with the cable mid-game and there is nothing useful
     * to do about it, so the exception is swallowed. `try`/`catch` rather than
     * `runCatching`, which would box a `Result` nobody reads.
     */
    fun sendNote(note: Int, isPressed: Boolean, channel: Int, velocity: Int) {
        val port = inputPort ?: return

        noteBuffer[0] = (((if (isPressed) NOTE_ON else NOTE_OFF) or channel) and 0xFF).toByte()
        noteBuffer[1] = note.toByte()
        noteBuffer[2] = velocity.toByte()

        try {
            port.send(noteBuffer, 0, MESSAGE_SIZE)
        } catch (_: Exception) {
            // Cable gone. Nothing to recover on this path.
        }
    }

    /**
     * Insurance against a note stuck on the game side when the app loses focus
     * with a finger still down.
     */
    fun allNotesOff(channel: Int) {
        val port = inputPort ?: return
        panicBuffer[0] = ((CONTROL_CHANGE or channel) and 0xFF).toByte()
        panicBuffer[1] = CC_ALL_NOTES_OFF.toByte()
        panicBuffer[2] = 0
        try {
            port.send(panicBuffer, 0, MESSAGE_SIZE)
        } catch (_: Exception) {
            // Same as above: the port is gone, and so are the notes.
        }
    }

    /** The field is cleared before the port closes, so no send races the close. */
    fun closeCurrentDevice() {
        val port = inputPort
        inputPort = null
        runCatching { port?.close() }
        runCatching { openedDevice?.close() }
        openedDevice = null
    }

    private fun failure(messageId: Int) = MidiOpenResult.Failure(context.getString(messageId))

    private companion object {
        const val MESSAGE_SIZE = 3
        const val NOTE_ON = 0x90
        const val NOTE_OFF = 0x80
        const val CONTROL_CHANGE = 0xB0
        const val CC_ALL_NOTES_OFF = 123
    }
}
