package sh.aminov.alternate.keypad

/**
 * Naming for MIDI note numbers.
 *
 * The settings screen shows both the name and the number — "C4 · 60". The
 * number is what the game's key binding actually matches on, and the name is
 * what makes it obvious that the two lanes sit a semitone apart rather than
 * an octave. Neither alone is enough.
 *
 * Scientific pitch notation, where note 60 is C4.
 */
object MidiNotes {

    private val NAMES = arrayOf(
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    )

    private const val NOTES_PER_OCTAVE = 12

    /** Octave of note 0. C4 is 60, so note 0 lands in octave -1. */
    private const val BASE_OCTAVE = -1

    fun name(note: Int): String {
        val index = ((note % NOTES_PER_OCTAVE) + NOTES_PER_OCTAVE) % NOTES_PER_OCTAVE
        val octave = Math.floorDiv(note, NOTES_PER_OCTAVE) + BASE_OCTAVE
        return "${NAMES[index]}$octave"
    }

    /** "C4 · 60" — the form used on the settings screen. */
    fun describe(note: Int): String = "${name(note)} · $note"
}
