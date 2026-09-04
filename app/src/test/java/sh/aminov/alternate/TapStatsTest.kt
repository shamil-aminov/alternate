package sh.aminov.alternate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sh.aminov.alternate.keypad.AlternateEngine
import sh.aminov.alternate.keypad.AlternateTuning

/**
 * Tests for the tempo readout.
 *
 * They go through [AlternateEngine] rather than poking `TapStats` directly:
 * `onTap` and `refresh` are internal on purpose, and driving them the way the
 * screen does — press, then tick — is also the only way to catch a mistake in
 * how the two halves are wired together.
 *
 * Time is fed in by hand, so these tests are exact and never flake.
 */
class TapStatsTest {

    private val tuning = AlternateTuning(debounceMs = 0L, chainWindowMs = 500L)

    /**
     * Taps at a fixed interval, ticking the frame loop after each one so the
     * derived values keep up.
     *
     * @return the engine, wound up to the requested tempo.
     */
    private fun engineTapping(intervalMs: Long, taps: Int): AlternateEngine {
        val engine = AlternateEngine(tuning)
        var time = 10_000L
        repeat(taps) { index ->
            engine.press(index.toLong(), time)
            engine.release(index.toLong())
            engine.tick(dtSeconds = intervalMs / 1000f, nowMs = time)
            time += intervalMs
        }
        return engine
    }

    @Test
    fun `a steady 50 ms interval reads as 20 taps per second`() {
        val engine = engineTapping(intervalMs = 50L, taps = 40)

        assertEquals(20f, engine.stats.tapsPerSecond, 0.5f)
    }

    /**
     * The number the whole app is built around. A 1/4 stream at 200 BPM is
     * 13.33 taps per second, which is a 75 ms interval.
     */
    @Test
    fun `a 75 ms interval reads as 200 BPM`() {
        val engine = engineTapping(intervalMs = 75L, taps = 40)

        assertEquals(200f, engine.stats.bpm, 5f)
    }

    @Test
    fun `a 60 ms interval reads as 250 BPM`() {
        val engine = engineTapping(intervalMs = 60L, taps = 40)

        assertEquals(250f, engine.stats.bpm, 6f)
    }

    /**
     * One stutter in the middle of an otherwise even stream is exactly what
     * the trimmed mean exists to absorb.
     */
    @Test
    fun `a single stuttered interval barely moves the tempo`() {
        val engine = AlternateEngine(tuning)
        var time = 10_000L

        // Ten clean taps at 50 ms, then one late one, then ten more.
        repeat(10) { index ->
            engine.press(index.toLong(), time)
            engine.release(index.toLong())
            engine.tick(0.05f, time)
            time += 50L
        }
        val steady = engine.stats.tapsPerSecond

        time += 120L
        engine.press(100L, time)
        engine.release(100L)
        engine.tick(0.12f, time)

        repeat(10) { index ->
            time += 50L
            engine.press(200L + index, time)
            engine.release(200L + index)
            engine.tick(0.05f, time)
        }

        assertEquals("the outlier should be trimmed away", steady, engine.stats.tapsPerSecond, 1.5f)
    }

    /**
     * The readout falls because silence caps it, not because a decay rate was
     * chosen: with no tap for a given stretch, the rate through that stretch
     * cannot exceed one tap in it, and the reading is held there. Past a
     * second the window is thrown away and the reading is zero.
     */
    @Test
    fun `a long pause clears the window instead of averaging across it`() {
        val engine = engineTapping(intervalMs = 50L, taps = 20)
        assertTrue(engine.stats.tapsPerSecond > 15f)

        var time = 10_000L + 20 * 50L
        repeat(FRAMES_IN_THREE_SECONDS) {
            time += FRAME_MS
            engine.tick(FRAME_SECONDS, time)
        }

        assertEquals("the readout should fall back to zero", 0f, engine.stats.tapsPerSecond, 0.01f)
        assertEquals(0, engine.stats.chain)
    }

    /**
     * Three quick taps used to be enough to put 500 BPM on the readout: two
     * intervals with nothing to compare them against, so the shorter one was
     * the measurement rather than a sample of it.
     */
    @Test
    fun `two intervals are not enough to report a tempo`() {
        val engine = AlternateEngine(tuning)

        var time = 10_000L
        repeat(3) { index ->
            engine.press(index.toLong(), time)
            engine.release(index.toLong())
            engine.tick(0.02f, time)
            time += 20L
        }

        assertEquals("three taps is not a tempo", 0f, engine.stats.bpm, 0f)
    }

    /**
     * One impossible gap among real ones — a finger rolling, or the glass
     * registering twice — is dropped rather than averaged in.
     */
    @Test
    fun `a freak interval in a small window is trimmed away`() {
        val engine = AlternateEngine(tuning)

        var time = 10_000L
        engine.press(0L, time)
        engine.release(0L)
        longArrayOf(75L, 8L, 75L, 75L).forEachIndexed { index, gap ->
            time += gap
            engine.press(index + 1L, time)
            engine.release(index + 1L)
            engine.tick(gap / 1000f, time)
        }

        assertEquals("the 8 ms gap should be thrown out", 200f, engine.stats.bpm, 10f)
    }

    /**
     * The point of capping by silence: it bites straight away and it scales.
     * Stop a 20 tap-per-second stream and a fifth of a second later the
     * reading is already a quarter of what it was, without waiting on a
     * grace period or a fixed slope.
     */
    @Test
    fun `the reading falls as soon as the tapping does`() {
        val engine = engineTapping(intervalMs = 50L, taps = 20)
        val streaming = engine.stats.tapsPerSecond
        assertTrue(streaming > 15f)

        var time = 10_000L + 20 * 50L
        repeat(FRAMES_IN_TWO_HUNDRED_MS) {
            time += FRAME_MS
            engine.tick(FRAME_SECONDS, time)
        }

        assertTrue("still reporting the old tempo", engine.stats.tapsPerSecond < streaming / 3f)
    }

    @Test
    fun `the first tap has no interval and no tempo`() {
        val engine = AlternateEngine(tuning)

        engine.press(1L, 10_000L)
        engine.tick(0.008f, 10_000L)

        assertEquals(1, engine.stats.total)
        assertEquals(0L, engine.stats.lastIntervalMs)
        assertEquals(0f, engine.stats.tapsPerSecond, 0f)
    }

    @Test
    fun `heat rises with tapping and decays with silence`() {
        val engine = engineTapping(intervalMs = 50L, taps = 20)
        assertTrue("tapping should build heat", engine.stats.heat > 0.5f)

        var time = 10_000L + 20 * 50L
        repeat(120) {
            time += 16L
            engine.tick(0.016f, time)
        }
        assertEquals(0f, engine.stats.heat, 0.01f)
    }

    private companion object {
        const val FRAME_MS = 16L
        const val FRAME_SECONDS = 0.016f
        const val FRAMES_IN_THREE_SECONDS = 188
        const val FRAMES_IN_TWO_HUNDRED_MS = 13
    }
}
