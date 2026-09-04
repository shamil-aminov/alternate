package sh.aminov.alternate.keypad

/**
 * Mapping from BPM to how full the gauge reads, 0..1.
 *
 * Piecewise linear, with bends on the tempos players already talk about. A
 * linear 0..260 bar would waste its bottom third — nobody streams below 100 —
 * and squeeze the stretch that decides whether you pass a map, 180 to 220,
 * into a few pixels. Each zone gets bar in proportion to how much it matters,
 * so the result reads like a tachometer with 220 up near the top.
 *
 * Plain JVM code with no Android dependency, so `BpmScaleTest` covers it.
 */
object BpmScale {

    /** Warm-up ends here. */
    const val WARMUP_TO = 140f

    /** Comfortable working tempo ends here. */
    const val WORKING_TO = 200f

    /** The limit for most players. */
    const val LIMIT_TO = 220f

    /** Top of the scale. There is headroom above the limit on purpose. */
    const val MAX = 260f

    /**
     * How many blocks the bar is divided into. Coarse on purpose: one block
     * has to be a visible step to an eye that is busy watching the game.
     */
    const val STEPS = 18

    /**
     * How many blocks are lit at [bpm], as a fraction. 11.4 means eleven full
     * and the twelfth 40% in — whole numbers would leave a tempo drifting
     * inside one step showing nothing until it crossed into the next.
     */
    fun stepsFor(bpm: Float): Float = (fillFor(bpm) * STEPS).coerceIn(0f, STEPS.toFloat())

    /** Share of the bar handed to each zone, cumulative. */
    private const val FILL_AT_WARMUP = 0.33f
    private const val FILL_AT_WORKING = 0.66f
    private const val FILL_AT_LIMIT = 0.88f

    /** @return how full the bar should be for [bpm], clamped to 0..1. */
    fun fillFor(bpm: Float): Float = when {
        bpm <= 0f -> 0f
        bpm < WARMUP_TO -> lerp(bpm, 0f, WARMUP_TO, 0f, FILL_AT_WARMUP)
        bpm < WORKING_TO -> lerp(bpm, WARMUP_TO, WORKING_TO, FILL_AT_WARMUP, FILL_AT_WORKING)
        bpm < LIMIT_TO -> lerp(bpm, WORKING_TO, LIMIT_TO, FILL_AT_WORKING, FILL_AT_LIMIT)
        bpm < MAX -> lerp(bpm, LIMIT_TO, MAX, FILL_AT_LIMIT, 1f)
        else -> 1f
    }

    private fun lerp(
        value: Float,
        fromLow: Float,
        fromHigh: Float,
        toLow: Float,
        toHigh: Float
    ): Float = toLow + (value - fromLow) / (fromHigh - fromLow) * (toHigh - toLow)
}
