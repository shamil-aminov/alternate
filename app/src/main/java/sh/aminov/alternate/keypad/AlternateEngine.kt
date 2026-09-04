package sh.aminov.alternate.keypad

/**
 * Alt-tap logic: every touch fires a note, alternating between two lanes, so
 * one finger produces the alternating key presses a stream needs.
 *
 * No Android or Compose dependency, so plain JVM tests cover it. Every method
 * runs on the touch thread; there is no synchronisation because the state is
 * primitive arrays and the path from [press] to the returned note allocates
 * nothing.
 */
class AlternateEngine(val tuning: AlternateTuning = AlternateTuning.Default) {

    private val heldPointers = LongArray(tuning.maxConcurrentNotes) { NO_POINTER }
    private val heldNotes = IntArray(tuning.maxConcurrentNotes) { NO_NOTE }
    private var lastPressMs: Long = NEVER

    /** Lane of the last accepted tap: 0 left, 1 right. */
    var lane: Int = LANE_RIGHT
        private set

    val stats = TapStats(tuning.chainWindowMs)

    /** Presses the debounce threw away. Read by the diagnostics overlay. */
    var droppedTaps: Int = 0
        private set

    val heldCount: Int
        get() {
            var count = 0
            for (i in heldPointers.indices) if (heldPointers[i] != NO_POINTER) count++
            return count
        }

    /**
     * @param eventTimeMs the input event's own timestamp, not the moment it was
     *   handled — it survives a busy UI thread.
     * @return the note to send, or [NO_NOTE] if the press was dropped.
     */
    fun press(pointerId: Long, eventTimeMs: Long): Int {
        val interval = if (lastPressMs == NEVER) {
            TapStats.NO_PREVIOUS_TAP
        } else {
            eventTimeMs - lastPressMs
        }
        if (interval < tuning.debounceMs) {
            droppedTaps++
            return NO_NOTE
        }

        val slot = freeSlot()
        if (slot == NO_SLOT) return NO_NOTE

        lane = 1 - lane
        val note = if (lane == LANE_LEFT) tuning.notePrimary else tuning.noteAlternate
        heldPointers[slot] = pointerId
        heldNotes[slot] = note
        lastPressMs = eventTimeMs
        stats.onTap(interval)
        return note
    }

    /** @return the note to silence, or [NO_NOTE] if this touch was dropped. */
    fun release(pointerId: Long): Int {
        for (i in heldPointers.indices) {
            if (heldPointers[i] == pointerId) {
                val note = heldNotes[i]
                heldPointers[i] = NO_POINTER
                heldNotes[i] = NO_NOTE
                return note
            }
        }
        return NO_NOTE
    }

    /** Silences everything held. Without it a note sticks down in the game. */
    inline fun releaseAll(onRelease: (Int) -> Unit) {
        for (i in 0 until tuning.maxConcurrentNotes) {
            val note = releaseSlot(i)
            if (note != NO_NOTE) onRelease(note)
        }
    }

    /** One frame of telemetry, off the hot path: the tempo maths happens here. */
    fun tick(dtSeconds: Float, nowMs: Long) {
        val since = if (lastPressMs == NEVER) Long.MAX_VALUE else nowMs - lastPressMs
        stats.refresh(dtSeconds, since)
    }

    /** Internal; public only so `inline fun releaseAll` can reach it. */
    @PublishedApi
    internal fun releaseSlot(slot: Int): Int {
        if (heldPointers[slot] == NO_POINTER) return NO_NOTE
        val note = heldNotes[slot]
        heldPointers[slot] = NO_POINTER
        heldNotes[slot] = NO_NOTE
        return note
    }

    private fun freeSlot(): Int {
        for (i in heldPointers.indices) {
            if (heldPointers[i] == NO_POINTER) return i
        }
        return NO_SLOT
    }

    companion object {
        /** The press was dropped, or the release found no match. */
        const val NO_NOTE = -1

        const val LANE_LEFT = 0
        const val LANE_RIGHT = 1

        private const val NO_POINTER = Long.MIN_VALUE
        private const val NO_SLOT = -1
        private const val NEVER = Long.MIN_VALUE
    }
}
