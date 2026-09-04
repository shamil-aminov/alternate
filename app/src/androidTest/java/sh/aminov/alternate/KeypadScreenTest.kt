package sh.aminov.alternate

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import sh.aminov.alternate.keypad.AlternateTuning
import sh.aminov.alternate.ui.screens.KeypadScreen
import sh.aminov.alternate.ui.theme.AlternateTheme

/**
 * The one thing the unit tests cannot prove: that a real touch on the real
 * composable reaches the engine and produces a note.
 *
 * Everything below the surface is covered by `AlternateEngineTest`, so this
 * stays deliberately thin — it checks the wiring, not the logic.
 */
class KeypadScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val tuning = AlternateTuning(debounceMs = 0L)

    @Test
    fun tappingTheScreenSendsAlternatingNotes() {
        val notesOn = mutableListOf<Int>()

        rule.setContent {
            AlternateTheme {
                KeypadScreen(
                    tuning = tuning,
                    liteGraphics = false,
                    showDiagnostics = false,
                    isDemo = false,
                    onNoteOn = { note -> notesOn += note },
                    onNoteOff = { },
                    onExit = { }
                )
            }
        }

        repeat(4) {
            rule.onRoot().performTouchInput { down(center); up() }
            rule.waitForIdle()
        }

        assertEquals(4, notesOn.size)
        notesOn.zipWithNext { previous, next ->
            assertTrue("two identical notes in a row: $notesOn", previous != next)
        }
        assertTrue(
            notesOn.all { it == tuning.notePrimary || it == tuning.noteAlternate }
        )
    }

    @Test
    fun everyNoteOnIsMatchedByANoteOff() {
        val notesOn = mutableListOf<Int>()
        val notesOff = mutableListOf<Int>()

        rule.setContent {
            AlternateTheme {
                KeypadScreen(
                    tuning = tuning,
                    liteGraphics = false,
                    showDiagnostics = false,
                    isDemo = false,
                    onNoteOn = { note -> notesOn += note },
                    onNoteOff = { note -> notesOff += note },
                    onExit = { }
                )
            }
        }

        repeat(3) {
            rule.onRoot().performTouchInput { down(center); up() }
            rule.waitForIdle()
        }

        assertEquals(notesOn.sorted(), notesOff.sorted())
    }
}
