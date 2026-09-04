package sh.aminov.alternate.data.models

import sh.aminov.alternate.keypad.AlternateTuning

/**
 * Everything the player can change, in one object.
 *
 * The engine settings live in [tuning]; the rest is how the app behaves
 * around the pad. Kept as one immutable value so a screen can be handed the
 * whole thing and never has to ask for pieces.
 *
 * @param liteGraphics drop the expensive effects. Fewer draw calls per frame
 *   means fewer dropped frames on a weaker phone, and a dropped frame is felt
 *   as input lag whether or not it really is any.
 * @param autoDoNotDisturb turn Do Not Disturb on while the pad is open. A
 *   heads-up notification steals focus mid-stream and ends the run.
 * @param showDiagnostics show the tap-interval overlay on the pad.
 * @param hasSeenOnboarding the first-run walkthrough has been completed. Read
 *   once at startup to pick the opening screen, never again.
 */
data class AppSettings(
    val tuning: AlternateTuning = AlternateTuning.Default,
    val keepScreenOn: Boolean = true,
    val hideSystemBars: Boolean = true,
    val liteGraphics: Boolean = false,
    val autoDoNotDisturb: Boolean = false,
    val showDiagnostics: Boolean = false,
    val hasSeenOnboarding: Boolean = false
)
