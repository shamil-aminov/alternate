package sh.aminov.alternate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sh.aminov.alternate.keypad.AlternateEngine
import sh.aminov.alternate.keypad.AlternateEngine.Companion.NO_NOTE
import sh.aminov.alternate.keypad.AlternateTuning

/**
 * Tests for the alternation itself.
 *
 * This used to be checked by hand: the app wrote a log to logcat, the log was
 * saved to a desktop, and a separate "test" parsed it. The same logic now
 * lives in a class with no Android dependency and is checked here — no phone,
 * no log, no manual steps.
 */
class AlternateEngineTest {

    private val tuning = AlternateTuning(debounceMs = 10L, chainWindowMs = 500L)

    /** Presses and releases one finger, as a real tap does. */
    private fun AlternateEngine.tap(id: Long, timeMs: Long): Int {
        val note = press(id, timeMs)
        release(id)
        return note
    }

    @Test
    fun `consecutive presses alternate between two notes`() {
        val engine = AlternateEngine(tuning)
        val notes = mutableListOf<Int>()

        var time = 1_000L
        repeat(8) { index ->
            notes += engine.tap(index.toLong(), time)
            time += 40L
        }

        notes.zipWithNext { previous, next ->
            assertNotEquals("two identical notes in a row: $notes", previous, next)
        }
        assertTrue(notes.all { it == tuning.notePrimary || it == tuning.noteAlternate })
    }

    @Test
    fun `a press inside the debounce window is dropped`() {
        val engine = AlternateEngine(tuning)

        val first = engine.press(pointerId = 1L, eventTimeMs = 1_000L)
        val bounce = engine.press(pointerId = 2L, eventTimeMs = 1_005L)

        assertNotEquals(NO_NOTE, first)
        assertEquals(NO_NOTE, bounce)
        assertEquals(1, engine.droppedTaps)
    }

    @Test
    fun `a dropped press does not shift the alternation`() {
        val engine = AlternateEngine(tuning)

        val first = engine.press(pointerId = 1L, eventTimeMs = 1_000L)
        engine.press(pointerId = 2L, eventTimeMs = 1_003L)
        engine.release(1L)
        val second = engine.press(pointerId = 3L, eventTimeMs = 1_100L)

        assertNotEquals(first, second)
    }

    @Test
    fun `a release returns the note its press produced`() {
        val engine = AlternateEngine(tuning)

        val note = engine.press(pointerId = 7L, eventTimeMs = 1_000L)

        assertEquals(note, engine.release(7L))
        assertEquals("releasing twice silences nothing", NO_NOTE, engine.release(7L))
    }

    @Test
    fun `presses beyond the concurrent limit are ignored`() {
        val engine = AlternateEngine(AlternateTuning(debounceMs = 0L, maxConcurrentNotes = 2))

        engine.press(pointerId = 1L, eventTimeMs = 1_000L)
        engine.press(pointerId = 2L, eventTimeMs = 1_050L)
        val third = engine.press(pointerId = 3L, eventTimeMs = 1_100L)

        assertEquals(NO_NOTE, third)
        assertEquals(2, engine.heldCount)
    }

    @Test
    fun `releaseAll silences every held note`() {
        val engine = AlternateEngine(tuning)
        engine.press(pointerId = 1L, eventTimeMs = 1_000L)
        engine.press(pointerId = 2L, eventTimeMs = 1_100L)

        val released = mutableListOf<Int>()
        engine.releaseAll { released += it }

        assertEquals(2, released.size)
        assertEquals(0, engine.heldCount)
    }

    @Test
    fun `the chain grows inside the window and breaks after a pause`() {
        val engine = AlternateEngine(tuning)

        var time = 1_000L
        repeat(5) { index ->
            engine.tap(index.toLong(), time)
            time += 50L
        }
        assertEquals(5, engine.stats.chain)

        engine.tap(99L, time + tuning.chainWindowMs + 100L)
        assertEquals("after a pause the chain starts over", 1, engine.stats.chain)
        assertEquals(6, engine.stats.total)
    }

    /**
     * The point of the split: `press` writes the interval down and gets out of
     * the way, and the tempo is only worked out on the next frame. Without a
     * `tick` the rate must stay where it was.
     */
    @Test
    fun `the tempo is not computed until the frame loop asks for it`() {
        val engine = AlternateEngine(tuning)

        var time = 1_000L
        repeat(10) { index ->
            engine.tap(index.toLong(), time)
            time += 50L
        }

        assertEquals("press alone must not do the maths", 0f, engine.stats.tapsPerSecond, 0f)

        engine.tick(dtSeconds = 0.008f, nowMs = time)
        assertTrue("the tick is what publishes it", engine.stats.tapsPerSecond > 0f)
    }
}
