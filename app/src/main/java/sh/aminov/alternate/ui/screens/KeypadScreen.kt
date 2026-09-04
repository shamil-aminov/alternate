package sh.aminov.alternate.ui.screens

import android.os.Build
import android.os.SystemClock
import android.view.InputDevice
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import sh.aminov.alternate.R
import sh.aminov.alternate.keypad.AlternateEngine
import sh.aminov.alternate.keypad.AlternateTuning
import sh.aminov.alternate.keypad.KeypadVisuals
import sh.aminov.alternate.keypad.Telemetry
import sh.aminov.alternate.keypad.snapshot
import sh.aminov.alternate.ui.hud.CockpitBackdrop
import sh.aminov.alternate.ui.hud.GaugePainter
import sh.aminov.alternate.ui.hud.HudGeometry
import sh.aminov.alternate.ui.hud.NeonText
import sh.aminov.alternate.ui.hud.drawBackdrop
import sh.aminov.alternate.ui.hud.drawCockpitFrame
import sh.aminov.alternate.ui.hud.drawCoreRing
import sh.aminov.alternate.ui.hud.drawEdgeFlash
import sh.aminov.alternate.ui.hud.drawSparks
import sh.aminov.alternate.ui.hud.drawWaves
import sh.aminov.alternate.ui.theme.Bone
import sh.aminov.alternate.ui.theme.ComboStyle
import sh.aminov.alternate.ui.theme.LabelStyle
import sh.aminov.alternate.ui.theme.ReadoutStyle
import sh.aminov.alternate.ui.theme.Steel
import sh.aminov.alternate.ui.theme.ZoneRed

/**
 * The pad. The whole screen is one button and every graphic is drawn, so
 * nothing intercepts a touch.
 *
 * On a tap the MIDI note goes out first and the animation is updated second:
 * whatever happens after the send cannot affect latency.
 *
 * @param isDemo no device is open — the pad works, the notes go nowhere.
 */
@Composable
fun KeypadScreen(
    tuning: AlternateTuning,
    liteGraphics: Boolean,
    showDiagnostics: Boolean,
    isDemo: Boolean,
    onNoteOn: (note: Int) -> Unit,
    onNoteOff: (note: Int) -> Unit,
    onExit: () -> Unit
) {
    val engine = remember(tuning) { AlternateEngine(tuning) }
    val visuals = remember(tuning, liteGraphics) { KeypadVisuals(liteGraphics) }
    val telemetry = remember { mutableStateOf(Telemetry()) }
    val exitArmed = remember { mutableStateOf(false) }

    val sendNoteOn by rememberUpdatedState(onNoteOn)
    val sendNoteOff by rememberUpdatedState(onNoteOff)

    RequestUnbufferedTouchDispatch()
    ReleaseNotesOnFocusLoss(engine) { note -> sendNoteOff(note) }

    // The one place all animation moves.
    LaunchedEffect(engine, visuals) {
        var previousFrameNanos = 0L
        var lastPublishNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                val dt = if (previousFrameNanos == 0L) {
                    0f
                } else {
                    ((frameNanos - previousFrameNanos) / NANOS_PER_SECOND)
                        .toFloat()
                        .coerceAtMost(MAX_STEP_SECONDS)
                }
                previousFrameNanos = frameNanos

                engine.tick(dt, SystemClock.uptimeMillis())
                visuals.advance(dt, engine.stats.bpm, engine.stats.heat)

                if (frameNanos - lastPublishNanos >= TELEMETRY_PERIOD_NANOS) {
                    lastPublishNanos = frameNanos
                    // From the visuals, not the raw stats, so the number and
                    // the bar always agree.
                    telemetry.value = engine.snapshot(visuals.smoothedBpm)
                }
            }
        }
    }

    // Leaving with a finger down would leave the note stuck in the game.
    DisposableEffect(engine) {
        onDispose { engine.releaseAll { note -> sendNoteOff(note) } }
    }

    BackHandler {
        if (exitArmed.value) onExit() else exitArmed.value = true
    }
    LaunchedEffect(exitArmed.value) {
        if (exitArmed.value) {
            delay(EXIT_ARM_WINDOW_MS)
            exitArmed.value = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(engine) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val changes = event.changes
                        for (i in changes.indices) {
                            val change = changes[i]
                            when {
                                change.changedToDownIgnoreConsumed() -> {
                                    val note = engine.press(change.id.value, change.uptimeMillis)
                                    if (note != AlternateEngine.NO_NOTE) {
                                        sendNoteOn(note)
                                        // After the send, and `size` read fresh
                                        // in case the screen has rotated.
                                        visuals.onTap(
                                            engine.lane,
                                            engine.stats.chain,
                                            HudGeometry.coreRadiusFor(
                                                size.width.toFloat(),
                                                size.height.toFloat()
                                            )
                                        )
                                    }
                                    change.consume()
                                }

                                change.changedToUpIgnoreConsumed() -> {
                                    val note = engine.release(change.id.value)
                                    if (note != AlternateEngine.NO_NOTE) sendNoteOff(note)
                                    change.consume()
                                }
                            }
                        }
                    }
                }
            }
    ) {
        ChromeLayer()
        LiveLayer(visuals, liteGraphics)

        MajorReadout(
            label = stringResource(R.string.readout_chain),
            value = { it.chain.toString() },
            telemetry = telemetry,
            visuals = visuals,
            lite = liteGraphics,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = EDGE_MARGIN_X, top = EDGE_MARGIN_Y)
        )
        MajorReadout(
            label = stringResource(R.string.readout_bpm),
            value = { formatBpm(it.bpm) },
            telemetry = telemetry,
            visuals = visuals,
            lite = liteGraphics,
            alignEnd = true,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = EDGE_MARGIN_X, top = EDGE_MARGIN_Y)
        )

        // A running count, not an event, so it does not punch.
        MinorReadout(
            label = stringResource(R.string.readout_total),
            value = { it.total.toString() },
            telemetry = telemetry,
            lite = liteGraphics,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = EDGE_MARGIN_X, bottom = EDGE_MARGIN_Y)
        )

        if (showDiagnostics) {
            Text(
                text = stringResource(
                    R.string.keypad_diagnostics,
                    telemetry.value.lastIntervalMs.toInt(),
                    telemetry.value.dropped,
                    telemetry.value.held
                ),
                style = LabelStyle,
                color = Steel,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = EDGE_MARGIN_Y)
            )
        }

        when {
            exitArmed.value -> TopBanner(stringResource(R.string.keypad_exit_hint), ZoneRed)
            isDemo -> TopBanner(stringResource(R.string.keypad_demo), Steel)
        }
    }
}

/** The only words on the pad, so one showing up reads as worth reading. */
@Composable
private fun BoxScope.TopBanner(text: String, color: Color) {
    Text(
        text = text,
        style = LabelStyle,
        color = color,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = EDGE_MARGIN_Y)
    )
}

/** A decimal here would only flicker. */
private fun formatBpm(bpm: Float): String = bpm.toInt().toString()

/** Unbuffered touch on Android 13+ saves as much as a whole frame. */
@Composable
private fun RequestUnbufferedTouchDispatch() {
    val view = LocalView.current
    LaunchedEffect(view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            view.requestUnbufferedDispatch(InputDevice.SOURCE_CLASS_POINTER)
        }
    }
}

/**
 * A heads-up notification steals focus without stopping the activity, so
 * `onStop` never fires and the pointer never comes back up. Any note held at
 * that moment would stay down in the game.
 */
@Composable
private fun ReleaseNotesOnFocusLoss(engine: AlternateEngine, onNoteOff: (Int) -> Unit) {
    val windowInfo = LocalWindowInfo.current
    val release by rememberUpdatedState(onNoteOff)
    LaunchedEffect(windowInfo, engine) {
        snapshotFlow { windowInfo.isWindowFocused }.collect { focused ->
            if (!focused) engine.releaseAll { note -> release(note) }
        }
    }
}

/** CHAIN and BPM: the numbers that punch forward on every tap. */
@Composable
private fun MajorReadout(
    label: String,
    value: (Telemetry) -> String,
    telemetry: State<Telemetry>,
    visuals: KeypadVisuals,
    lite: Boolean,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false
) {
    val origin = if (alignEnd) TransformOrigin(1f, 0.5f) else TransformOrigin(0f, 0.5f)
    Column(
        modifier = if (lite) modifier else modifier.punch(visuals, COMBO_PUNCH, origin),
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Text(text = label, style = LabelStyle, color = Steel)
        NeonText(
            text = value(telemetry.value),
            style = ComboStyle,
            color = Bone,
            glow = READOUT_GLOW,
            glowRadius = MAJOR_GLOW_RADIUS,
            lite = lite
        )
    }
}

/**
 * A corner readout. Every one on this screen is a [Steel] label over a [Bone]
 * value; the accent is spent on the things that are actually live.
 */
@Composable
private fun MinorReadout(
    label: String,
    value: (Telemetry) -> String,
    telemetry: State<Telemetry>,
    lite: Boolean,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Text(text = label, style = LabelStyle, color = Steel)
        NeonText(
            text = value(telemetry.value),
            style = ReadoutStyle,
            color = Bone,
            glow = MINOR_GLOW,
            glowRadius = MINOR_GLOW_RADIUS,
            lite = lite
        )
    }
}

/**
 * Field, vignette and the unlit gauge. Reads no animated value, so the draw
 * list is recorded once per resize and replayed.
 */
@Composable
private fun ChromeLayer() {
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { }
            .drawWithCache {
                val geometry = HudGeometry(size, density)
                val backdrop = CockpitBackdrop(geometry)
                val gauge = GaugePainter(geometry)
                onDrawBehind {
                    drawBackdrop()
                    drawCockpitFrame(backdrop)
                    // After the vignette: the gauge sits where the vignette is
                    // darkest, and underneath it the empty scale disappears.
                    gauge.drawBed(this)
                }
            }
    )
}

/** Everything that answers the finger, in one draw zone. */
@Composable
private fun LiveLayer(visuals: KeypadVisuals, liteGraphics: Boolean) {
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { }
            .drawWithCache {
                val geometry = HudGeometry(size, density)
                val backdrop = CockpitBackdrop(geometry)
                val gauge = GaugePainter(geometry)
                onDrawBehind {
                    // The app's only frame subscription, read in the draw
                    // phase so a frame never becomes a recomposition.
                    visuals.clock.floatValue
                    drawEdgeFlash(geometry, visuals, backdrop)
                    drawWaves(geometry, visuals)
                    if (!liteGraphics) drawSparks(geometry, visuals)
                    drawCoreRing(geometry, visuals, liteGraphics)
                    gauge.drawFill(this, visuals, liteGraphics)
                }
            }
    )
}

/**
 * The punch on each tap. `graphicsLayer` reads the state itself, so the spring
 * runs frame by frame without dragging the text into recomposition. The origin
 * is pinned to the screen edge so the number grows inwards.
 */
private fun Modifier.punch(
    visuals: KeypadVisuals,
    amount: Float,
    origin: TransformOrigin
): Modifier = graphicsLayer {
    val scale = 1f + visuals.punch.floatValue * amount
    scaleX = scale
    scaleY = scale
    transformOrigin = origin
}

private val EDGE_MARGIN_X = 56.dp
private val EDGE_MARGIN_Y = 40.dp
private val MAJOR_GLOW_RADIUS = 22.dp
private val MINOR_GLOW_RADIUS = 12.dp

private val READOUT_GLOW = Color(0xB3BFE8FF)
private val MINOR_GLOW = Color(0x66A8D0F0)

private const val COMBO_PUNCH = 0.16f
private const val NANOS_PER_SECOND = 1_000_000_000.0

/** Two frames at 60 Hz. Longer and the stiff gauge springs would ring. */
private const val MAX_STEP_SECONDS = 0.032f

private const val TELEMETRY_PERIOD_NANOS = 100_000_000L
private const val EXIT_ARM_WINDOW_MS = 2_000L
