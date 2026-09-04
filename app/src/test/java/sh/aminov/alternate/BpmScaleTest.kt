package sh.aminov.alternate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sh.aminov.alternate.keypad.BpmScale

/**
 * Tests for the gauge scale.
 *
 * The complaint that produced this class was that the bar felt wrong: empty
 * at a tempo that is not slow, and not full at a tempo that is very fast.
 * These tests pin the behaviour that fixes it.
 */
class BpmScaleTest {

    @Test
    fun `an empty gauge means no tapping`() {
        assertEquals(0f, BpmScale.fillFor(0f), 0f)
        assertEquals(0f, BpmScale.fillFor(-10f), 0f)
    }

    @Test
    fun `the scale saturates at the top and stays there`() {
        assertEquals(1f, BpmScale.fillFor(BpmScale.MAX), 0f)
        assertEquals(1f, BpmScale.fillFor(400f), 0f)
    }

    @Test
    fun `the zone boundaries land on their anchors`() {
        assertEquals(0.33f, BpmScale.fillFor(BpmScale.WARMUP_TO), 0.001f)
        assertEquals(0.66f, BpmScale.fillFor(BpmScale.WORKING_TO), 0.001f)
        assertEquals(0.88f, BpmScale.fillFor(BpmScale.LIMIT_TO), 0.001f)
    }

    /** The original complaint: 220 and above must read as nearly full. */
    @Test
    fun `over the limit reads as nearly full`() {
        assertTrue("220 BPM should be near the top", BpmScale.fillFor(220f) >= 0.85f)
        assertTrue("240 BPM should be nearer still", BpmScale.fillFor(240f) >= 0.9f)
    }

    /** The other half of it: an ordinary tempo must be visibly on the bar. */
    @Test
    fun `a moderate tempo is visibly on the scale`() {
        val fill = BpmScale.fillFor(120f)
        assertTrue("120 BPM should not read as empty, was $fill", fill > 0.2f)
        assertTrue("nor as fast, was $fill", fill < 0.4f)
    }

    @Test
    fun `the scale never goes backwards`() {
        var previous = -1f
        var bpm = 0f
        while (bpm <= 300f) {
            val fill = BpmScale.fillFor(bpm)
            assertTrue("fill dropped at $bpm BPM", fill >= previous)
            assertTrue("fill left 0..1 at $bpm BPM", fill in 0f..1f)
            previous = fill
            bpm += 1f
        }
    }

    /**
     * The zone that decides whether a map is passable gets more bar than its
     * BPM range would buy on a linear scale. That is the entire point of the
     * piecewise curve, so it is worth asserting rather than assuming.
     */
    @Test
    fun `the limit zone is denser than the warm-up zone`() {
        val warmupPerBpm = BpmScale.fillFor(BpmScale.WARMUP_TO) / BpmScale.WARMUP_TO
        val limitSpan = BpmScale.fillFor(BpmScale.LIMIT_TO) - BpmScale.fillFor(BpmScale.WORKING_TO)
        val limitPerBpm = limitSpan / (BpmScale.LIMIT_TO - BpmScale.WORKING_TO)

        assertTrue(
            "the top of the scale should resolve finer than the bottom",
            limitPerBpm > warmupPerBpm
        )
    }
}
