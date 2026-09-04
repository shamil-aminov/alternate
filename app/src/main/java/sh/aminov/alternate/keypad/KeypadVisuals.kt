package sh.aminov.alternate.keypad

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Animation state of the pad.
 *
 * Nothing moves by itself except the orbital brackets, and those stop once the
 * pad goes idle. [clock] and [punch] are the only Compose states, both written
 * from the frame loop and read in the draw phase, so a frame never causes a
 * recomposition.
 */
@Stable
class KeypadVisuals(private val liteGraphics: Boolean = false) {

    /** Seconds since the screen opened. The canvas redraw trigger. */
    val clock = mutableFloatStateOf(0f)

    /** Counter punch, 0..1. Read by the text layer rather than the canvas. */
    val punch = mutableFloatStateOf(0f)

    /** Side of the last tap: 0 left, 1 right. */
    var side: Int = 0
        private set

    /** Core glow after a tap, 0..1. */
    var corePulse: Float = 0f
        private set

    val edgeFlash = FloatArray(SIDES)
    val waves = WavePool(capacity = 20, lifetimeSeconds = 1.0f)
    val sparks = SparkPool(capacity = 56, lifetimeSeconds = 0.55f)

    /** The one display value: gauge and readout both take it from here. */
    var smoothedBpm: Float = 0f
        private set

    var smoothedHeat: Float = 0f
        private set

    var orbitalRotation: Float = 0f
        private set

    var secondsSinceTap: Float = Float.MAX_VALUE
        private set

    /** 1..0. Falls after [IDLE_AFTER_SECONDS] so a resting pad stops burning in. */
    var wakefulness: Float = 0f
        private set

    private var punchVelocity = 0f

    /** Lit level per gauge block, with spring velocity. Overshoots past 1. */
    private val cellLevel = FloatArray(BpmScale.STEPS)
    private val cellVelocity = FloatArray(BpmScale.STEPS)

    fun gaugeCell(index: Int): Float = cellLevel[index]

    fun onTap(side: Int, chain: Int, coreRadius: Float) {
        this.side = side
        corePulse = 1f
        edgeFlash[side] = 1f
        secondsSinceTap = 0f
        wakefulness = 1f

        // A round chain lights both edges — the only moment the screen is
        // symmetric. Two float writes, so it survives into lite mode.
        if (chain > 0 && chain % MILESTONE_EVERY == 0) {
            for (i in edgeFlash.indices) edgeFlash[i] = 1f
        }

        // Lite mode keeps the core pulse and the edge flash and drops the rest.
        // The punch is the worst of it: it writes a Compose state every frame
        // while it settles, invalidating the text layer over and over.
        if (liteGraphics) return

        punch.floatValue = 1f
        punchVelocity = 0f
        waves.spawn(side)
        sparks.burst(side, coreRadius)
    }

    fun advance(dt: Float, targetBpm: Float, targetHeat: Float) {
        corePulse = (corePulse - dt * PULSE_DECAY).coerceAtLeast(0f)
        for (i in edgeFlash.indices) {
            edgeFlash[i] = (edgeFlash[i] - dt * EDGE_DECAY).coerceAtLeast(0f)
        }
        if (!liteGraphics) {
            waves.advance(dt)
            sparks.advance(dt)
        }
        clock.floatValue += dt

        if (secondsSinceTap < Float.MAX_VALUE) secondsSinceTap += dt
        wakefulness = if (secondsSinceTap < IDLE_AFTER_SECONDS) {
            1f
        } else {
            (1f - (secondsSinceTap - IDLE_AFTER_SECONDS) / IDLE_FADE_SECONDS).coerceAtLeast(0f)
        }

        // Rising faster than falling, both short enough that a tap moves the
        // bar rather than nudging an average of the last few.
        val bpmTau = if (targetBpm > smoothedBpm) BPM_ATTACK_TAU else BPM_RELEASE_TAU
        smoothedBpm += (targetBpm - smoothedBpm) * approach(dt, bpmTau)
        smoothedHeat += (targetHeat - smoothedHeat) * approach(dt, HEAT_TAU)

        advanceGauge(dt)
        orbitalRotation += dt * (ORBIT_BASE_SPEED + smoothedHeat * ORBIT_MAX_ACCEL)
        advancePunch(dt)
    }

    /**
     * Springs each block towards lit or unlit. Blocks below the level are
     * full, the one straddling it takes the remainder — that fraction is what
     * lets the top block answer to changes far smaller than a block.
     */
    private fun advanceGauge(dt: Float) {
        val lit = BpmScale.stepsFor(smoothedBpm)
        for (i in cellLevel.indices) {
            val target = (lit - i).coerceIn(0f, 1f)
            val level = cellLevel[i]
            if (level == target && cellVelocity[i] == 0f) continue

            cellVelocity[i] +=
                (-CELL_STIFFNESS * (level - target) - CELL_DAMPING * cellVelocity[i]) * dt
            val next = level + cellVelocity[i] * dt

            if (abs(next - target) < CELL_REST && abs(cellVelocity[i]) < CELL_REST) {
                cellLevel[i] = target
                cellVelocity[i] = 0f
            } else {
                cellLevel[i] = next
            }
        }
    }

    /**
     * The counter punch as a spring. It undershoots past its resting size,
     * which is what makes the number read as struck rather than resized.
     *
     * Semi-implicit Euler — velocity first — because it stays stable at the
     * longest step the frame loop allows.
     */
    private fun advancePunch(dt: Float) {
        val current = punch.floatValue
        if (current == 0f && punchVelocity == 0f) return

        punchVelocity += (-PUNCH_STIFFNESS * current - PUNCH_DAMPING * punchVelocity) * dt
        val next = current + punchVelocity * dt

        if (abs(next) < PUNCH_REST && abs(punchVelocity) < PUNCH_REST) {
            punchVelocity = 0f
            punch.floatValue = 0f
            return
        }
        punch.floatValue = next
    }

    private companion object {
        const val SIDES = 2
        const val PULSE_DECAY = 4.5f
        const val EDGE_DECAY = 3.2f

        const val PUNCH_STIFFNESS = 420f
        const val PUNCH_DAMPING = 26f
        const val PUNCH_REST = 0.001f

        /** Underdamped: a block flares about a tenth past full and settles. */
        const val CELL_STIFFNESS = 2200f
        const val CELL_DAMPING = 46f
        const val CELL_REST = 0.002f

        const val MILESTONE_EVERY = 50

        /** Seconds to cover 63% of the distance to the target. */
        const val BPM_ATTACK_TAU = 0.03f
        const val BPM_RELEASE_TAU = 0.04f
        const val HEAT_TAU = 0.12f

        const val IDLE_AFTER_SECONDS = 5f
        const val IDLE_FADE_SECONDS = 1.5f

        const val ORBIT_BASE_SPEED = 0.5f
        const val ORBIT_MAX_ACCEL = 4.0f

        /** Frame-rate independent: same wall-clock time at 60 and at 120 Hz. */
        fun approach(dt: Float, tau: Float): Float = 1f - exp(-dt / tau)
    }
}

/**
 * Waves on primitive arrays. Slots are reused in a ring and the oldest is
 * evicted by the newest, so neither a tap nor a frame creates an object.
 */
class WavePool(val capacity: Int, private val lifetimeSeconds: Float) {

    private val side = IntArray(capacity)
    private val age = FloatArray(capacity) { FREE }
    private var next = 0

    fun spawn(side: Int) {
        val slot = next
        next = (next + 1) % capacity
        this.side[slot] = side
        age[slot] = 0f
    }

    fun advance(dt: Float) {
        for (i in age.indices) {
            if (age[i] == FREE) continue
            age[i] += dt
            if (age[i] >= lifetimeSeconds) age[i] = FREE
        }
    }

    fun isAlive(index: Int): Boolean = age[index] != FREE

    /** 0..1 along its path. */
    fun progress(index: Int): Float = age[index] / lifetimeSeconds

    fun sideOf(index: Int): Int = side[index]

    private companion object {
        const val FREE = Float.MAX_VALUE
    }
}

/**
 * Sparks flung from under the core. Offsets and velocities are stored, screen
 * coordinates worked out at draw time, so the pool survives a rotation.
 *
 * Randomness is a private xorshift rather than `kotlin.random`: the burst runs
 * on the touch thread five times a tap, and the shared generator is
 * synchronised.
 */
class SparkPool(val capacity: Int, private val lifetimeSeconds: Float) {

    private val x = FloatArray(capacity)
    private val y = FloatArray(capacity)
    private val vx = FloatArray(capacity)
    private val vy = FloatArray(capacity)
    private val side = IntArray(capacity)
    private val age = FloatArray(capacity) { FREE }
    private var next = 0
    private var randomState = SEED

    fun burst(side: Int, coreRadius: Float) {
        val axis = if (side == 0) PI else 0f
        repeat(SPARKS_PER_TAP) {
            val angle = axis + (nextFloat() - 0.5f) * SPREAD
            val speed = MIN_SPEED + nextFloat() * (MAX_SPEED - MIN_SPEED)
            val slot = next
            next = (next + 1) % capacity
            x[slot] = cos(angle) * coreRadius
            y[slot] = sin(angle) * coreRadius
            vx[slot] = cos(angle) * speed
            vy[slot] = sin(angle) * speed
            this.side[slot] = side
            age[slot] = 0f
        }
    }

    fun advance(dt: Float) {
        for (i in age.indices) {
            if (age[i] == FREE) continue
            age[i] += dt
            if (age[i] >= lifetimeSeconds) {
                age[i] = FREE
                continue
            }
            x[i] += vx[i] * dt
            y[i] += vy[i] * dt
            // Drag, so a spark runs out of breath rather than flying flat.
            val damp = 1f - (DRAG_PER_SECOND * dt).coerceAtMost(1f)
            vx[i] *= damp
            vy[i] *= damp
        }
    }

    fun isAlive(index: Int): Boolean = age[index] != FREE

    fun progress(index: Int): Float = age[index] / lifetimeSeconds

    fun offsetX(index: Int): Float = x[index]

    fun offsetY(index: Int): Float = y[index]

    fun velocityX(index: Int): Float = vx[index]

    fun velocityY(index: Int): Float = vy[index]

    fun sideOf(index: Int): Int = side[index]

    /** xorshift32, top 24 bits scaled into 0..1. */
    private fun nextFloat(): Float {
        var state = randomState
        state = state xor (state shl 13)
        state = state xor (state ushr 17)
        state = state xor (state shl 5)
        randomState = state
        return (state ushr 8) * FLOAT_SCALE
    }

    private companion object {
        const val FREE = Float.MAX_VALUE
        const val SPARKS_PER_TAP = 5
        const val PI = 3.14159265f

        const val SEED = 0x2545F491
        const val FLOAT_SCALE = 1f / (1 shl 24)

        /** Spread around the side axis, radians. */
        const val SPREAD = 1.15f
        const val MIN_SPEED = 540f
        const val MAX_SPEED = 1550f
        const val DRAG_PER_SECOND = 2.4f
    }
}
