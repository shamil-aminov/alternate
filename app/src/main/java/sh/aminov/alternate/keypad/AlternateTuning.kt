package sh.aminov.alternate.keypad

/**
 * Every knob of the pad in one place, so tuning it by feel does not turn
 * into a hunt through the source.
 *
 * @param notePrimary note of the left lane.
 * @param noteAlternate note of the right lane.
 * @param midiChannel MIDI channel, 0-based (2 means "channel 3" in most UIs).
 * @param velocity key velocity; rhythm games ignore it, hence the maximum.
 * @param debounceMs shortest accepted gap between taps. It swallows the
 *   double hit of a finger rolling off the glass, and must stay well under
 *   the period of real alternate tapping — 20 taps per second is a 50 ms
 *   period, so anything up to ~30 ms is safe.
 * @param maxConcurrentNotes how many notes may sound at once.
 * @param chainWindowMs pause after which a chain of taps counts as broken.
 */
data class AlternateTuning(
    val notePrimary: Int = DEFAULT_NOTE_PRIMARY,
    val noteAlternate: Int = DEFAULT_NOTE_ALTERNATE,
    val midiChannel: Int = DEFAULT_MIDI_CHANNEL,
    val velocity: Int = DEFAULT_VELOCITY,
    val debounceMs: Long = DEFAULT_DEBOUNCE_MS,
    val maxConcurrentNotes: Int = 2,
    val chainWindowMs: Long = TapStats.DEFAULT_CHAIN_WINDOW_MS
) {
    companion object {
        /** Shared default for the engine and the MIDI sender. */
        val Default = AlternateTuning()

        const val DEFAULT_NOTE_PRIMARY = 60
        const val DEFAULT_NOTE_ALTERNATE = 61
        const val DEFAULT_MIDI_CHANNEL = 2
        const val DEFAULT_VELOCITY = 127
        const val DEFAULT_DEBOUNCE_MS = 12L

        /** Bounds offered by the settings screen. */
        const val MIN_DEBOUNCE_MS = 0L
        const val MAX_DEBOUNCE_MS = 30L
        const val MIN_NOTE = 0
        const val MAX_NOTE = 127
        const val MIN_CHAIN_WINDOW_MS = 200L
        const val MAX_CHAIN_WINDOW_MS = 2_000L
    }
}
