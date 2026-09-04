package sh.aminov.alternate.keypad

/**
 * Snapshot of the telemetry shown on screen.
 *
 * Text is the one part of the panel a redraw alone cannot update: changing a
 * string means recomposition and a fresh layout pass. So the readings are
 * sampled ten times a second rather than every frame — nobody reads them
 * faster than that, and the frame stays free.
 *
 * @param lastIntervalMs gap before the last accepted tap. Diagnostics only.
 * @param dropped presses the debounce threw away. Diagnostics only.
 * @param held notes down at the moment of the snapshot. Diagnostics only.
 */
data class Telemetry(
    val bpm: Float = 0f,
    val chain: Int = 0,
    val total: Int = 0,
    val lastIntervalMs: Long = 0L,
    val dropped: Int = 0,
    val held: Int = 0
)

/**
 * @param displayBpm the smoothed value the gauge is drawing. Passed in rather
 *   than read from [TapStats] so the number and the bar never disagree, and
 *   so the text layer never touches a per-frame state and drags itself into
 *   recomposition.
 */
fun AlternateEngine.snapshot(displayBpm: Float): Telemetry = Telemetry(
    bpm = displayBpm,
    chain = stats.chain,
    total = stats.total,
    lastIntervalMs = stats.lastIntervalMs,
    dropped = droppedTaps,
    held = heldCount
)
