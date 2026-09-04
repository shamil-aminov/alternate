package sh.aminov.alternate.keypad

/**
 * Tap counters and the tempo readout.
 *
 * [onTap] runs on the touch thread just before the MIDI note goes out, so it
 * only records. The arithmetic waits for [refresh], once per frame. Nothing
 * here allocates.
 */
class TapStats(private val chainWindowMs: Long = DEFAULT_CHAIN_WINDOW_MS) {

    var total: Int = 0
        private set

    /** Current unbroken chain. */
    var chain: Int = 0
        private set

    /** Gap before the last tap, ms. Zero before the second tap. */
    var lastIntervalMs: Long = 0L
        private set

    private val intervals = FloatArray(BUFFER_SIZE)

    /** Sorted here, so the window itself keeps its arrival order. */
    private val scratch = FloatArray(BUFFER_SIZE)

    private var intervalCount = 0
    private var nextIndex = 0
    private var windowDirty = false

    var tapsPerSecond: Float = 0f
        private set

    /** Tempo as a 1/4 stream, the way osu! labels maps. */
    val bpm: Float get() = tapsPerSecond * BEATS_PER_TAP

    /** 0..1 burst energy, driving the faster animations. */
    var heat: Float = 0f
        private set

    /** Hot path: O(1) and allocation-free. [NO_PREVIOUS_TAP] for the first tap. */
    internal fun onTap(intervalMs: Long) {
        total++
        lastIntervalMs = if (intervalMs == NO_PREVIOUS_TAP) 0L else intervalMs
        chain = if (intervalMs <= chainWindowMs) chain + 1 else 1

        if (intervalMs > MAX_GAP_MS) {
            // A new burst: the old window describes a different tempo.
            intervalCount = 0
            nextIndex = 0
        } else if (intervalMs in 1..MAX_TRACKED_INTERVAL_MS) {
            intervals[nextIndex] = intervalMs / MILLIS_PER_SECOND
            nextIndex = (nextIndex + 1) % BUFFER_SIZE
            if (intervalCount < BUFFER_SIZE) intervalCount++
            windowDirty = true
        }

        heat = (heat + HEAT_PER_TAP).coerceAtMost(1f)
    }

    /** One frame of derived values. Never called from the touch thread. */
    internal fun refresh(dtSeconds: Float, sinceLastTapMs: Long) {
        if (windowDirty) {
            windowDirty = false
            recomputeRate()
        }

        heat = (heat - dtSeconds * HEAT_DECAY_PER_SECOND).coerceAtLeast(0f)

        // Silence caps the rate: with no tap for this long, the rate through
        // that stretch cannot exceed one tap in it. No decay constant needed —
        // the reading falls faster the faster the player was going.
        if (sinceLastTapMs > 0) {
            val ceiling = MILLIS_PER_SECOND / sinceLastTapMs
            if (ceiling < tapsPerSecond) tapsPerSecond = ceiling
        }
        if (sinceLastTapMs > MAX_TRACKED_INTERVAL_MS) {
            tapsPerSecond = 0f
            intervalCount = 0
            nextIndex = 0
        }
        if (sinceLastTapMs > chainWindowMs) chain = 0
    }

    /**
     * Trimmed mean, then exponential smoothing. Below [MIN_SAMPLES] there is
     * nothing to compare a gap against, so nothing is reported: two taps and a
     * twitch used to read as 500 BPM.
     */
    private fun recomputeRate() {
        if (intervalCount < MIN_SAMPLES) return

        // Insertion sort into scratch: at four elements it beats anything
        // smarter, and unlike sorting a copy it allocates nothing.
        for (i in 0 until intervalCount) {
            val value = intervals[i]
            var j = i - 1
            while (j >= 0 && scratch[j] > value) {
                scratch[j + 1] = scratch[j]
                j--
            }
            scratch[j + 1] = value
        }

        // Drop the fastest and slowest. At exactly three that leaves the median.
        val from = if (intervalCount >= TRIM_FROM_COUNT) 1 else 0
        val until = if (intervalCount >= TRIM_FROM_COUNT) intervalCount - 1 else intervalCount

        var sum = 0f
        for (i in from until until) sum += scratch[i]
        val averageInterval = sum / (until - from)
        if (averageInterval <= 0f) return

        val rawRate = 1f / averageInterval
        tapsPerSecond = if (tapsPerSecond == 0f) {
            rawRate
        } else {
            ALPHA * rawRate + (1f - ALPHA) * tapsPerSecond
        }
    }

    companion object {
        const val NO_PREVIOUS_TAP = Long.MAX_VALUE
        const val DEFAULT_CHAIN_WINDOW_MS = 700L

        /** 60 seconds over 4 notes per beat: one tap a second is 15 BPM. */
        const val BEATS_PER_TAP = 15f

        private const val MILLIS_PER_SECOND = 1000f

        /** About a third of a second at stream speed. */
        private const val BUFFER_SIZE = 4

        /** Weight of the newest measurement. */
        private const val ALPHA = 0.7f

        private const val MIN_SAMPLES = 3
        private const val TRIM_FROM_COUNT = 3

        /** A gap this long ends the burst. */
        private const val MAX_GAP_MS = 500L

        /** Longer than this is not tapping, it is a fresh start. */
        private const val MAX_TRACKED_INTERVAL_MS = 1000L

        private const val HEAT_PER_TAP = 0.14f
        private const val HEAT_DECAY_PER_SECOND = 1.5f
    }
}
