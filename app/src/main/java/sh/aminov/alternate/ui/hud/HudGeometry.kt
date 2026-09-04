package sh.aminov.alternate.ui.hud

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.min

/**
 * Layout of the keypad screen.
 *
 * The core sits dead centre, readouts go to the corners; spacing inside a pair
 * is tight and the margin around it several times larger, so each pair reads
 * as one object. Everything is a fraction of the screen rather than a fixed
 * dp, because a landscape phone is short and 150 dp from the top lands
 * somewhere else on a tablet. Computed once per resize in `drawWithCache`.
 */
class HudGeometry(size: Size, val density: Float) {

    val width = size.width
    val height = size.height

    fun dp(value: Float): Float = value * density

    val centerX = width / 2f
    val centerY = height / 2f
    val center = Offset(centerX, centerY)

    /** Core radius. The main object on the screen. */
    val coreRadius = coreRadiusFor(width, height)

    /** Where a wave begins its path. */
    val waveStart = coreRadius + dp(WAVE_GAP_DP)

    /** Where it ends: past the screen edge, so it never stops with a jolt. */
    val waveEnd = width * WAVE_END_RATIO

    /**
     * Half-height of the wave at the core. Kept under the core radius so the
     * waves stay clear of the corner readouts.
     */
    val waveExtent = coreRadius * WAVE_EXTENT_RATIO

    /** Width of the band that lights up when a side fires. */
    val edgeFlashWidth = width * EDGE_FLASH_RATIO

    /** Margin for the corner readouts, capped so a small screen keeps its width. */
    val marginX = min(dp(MARGIN_X_DP), width * MARGIN_X_MAX_RATIO)

    /** Tempo gauge: a tall bar down the right-hand side. */
    val gaugeWidth = dp(GAUGE_WIDTH_DP)
    val gaugeRight = width - marginX

    /**
     * The bar starts below the BPM readout, which with a 68 sp number takes
     * about a third of the height. Together they read as one instrument.
     */
    val gaugeTop = height * GAUGE_TOP_RATIO
    val gaugeBottom = height * GAUGE_BOTTOM_RATIO
    val gaugeHeight = gaugeBottom - gaugeTop

    companion object {

        /** Core radius as a fraction of the shorter side: drawing and sparks share it. */
        const val CORE_RADIUS_RATIO = 0.19f

        fun coreRadiusFor(width: Float, height: Float): Float =
            min(width, height) * CORE_RADIUS_RATIO

        private const val WAVE_GAP_DP = 26f
        private const val WAVE_END_RATIO = 0.46f
        private const val WAVE_EXTENT_RATIO = 0.92f
        private const val EDGE_FLASH_RATIO = 0.14f

        private const val MARGIN_X_DP = 56f
        private const val MARGIN_X_MAX_RATIO = 0.08f

        private const val GAUGE_WIDTH_DP = 54f
        private const val GAUGE_TOP_RATIO = 0.36f
        private const val GAUGE_BOTTOM_RATIO = 0.92f
    }
}
