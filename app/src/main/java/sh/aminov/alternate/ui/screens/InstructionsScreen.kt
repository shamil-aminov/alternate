package sh.aminov.alternate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sh.aminov.alternate.R
import sh.aminov.alternate.ui.hud.HudButton
import sh.aminov.alternate.ui.hud.HudTopBar
import sh.aminov.alternate.ui.hud.cockpitBackdrop
import sh.aminov.alternate.ui.theme.Accent
import sh.aminov.alternate.ui.theme.AccentDeep
import sh.aminov.alternate.ui.theme.BodyStyle
import sh.aminov.alternate.ui.theme.Bone
import sh.aminov.alternate.ui.theme.ButtonSmallStyle
import sh.aminov.alternate.ui.theme.ComboStyle
import sh.aminov.alternate.ui.theme.DisplayStyle
import sh.aminov.alternate.ui.theme.LabelStyle
import sh.aminov.alternate.ui.theme.ReadoutStyle
import sh.aminov.alternate.ui.theme.Steel

/**
 * How it works, one step at a time — and, on a first run, the whole setup.
 *
 * A carousel rather than a page of panels: this is read once, in order, by
 * someone who has not got the thing working yet, and a wall of five boxes
 * invites skimming. The order is the order the problems actually arrive in —
 * cable, the game's own setting, the binding, the notification that ruins a
 * run, and only then the pad. That fourth step carries the permission button
 * itself, offered where it makes sense rather than found later under a name
 * nobody recognises.
 *
 * @param isOnboarding first run: nothing to go back to, so the chevron is gone
 *   and the last step commits instead.
 */
@Composable
fun InstructionsScreen(
    onBack: () -> Unit,
    isOnboarding: Boolean = false,
    isDoNotDisturbGranted: Boolean = false,
    onGrantDoNotDisturbAccess: () -> Unit = {}
) {
    val steps = remember { STEPS }
    val pagerState = rememberPagerState { steps.size }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().cockpitBackdrop()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = SCREEN_PADDING, vertical = 16.dp)
        ) {
            if (isOnboarding) {
                Spacer(Modifier.height(TOP_BAR_HEIGHT))
            } else {
                HudTopBar(onBack = onBack)
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                pageSpacing = PAGE_SPACING
            ) { page ->
                StepPage(
                    step = steps[page],
                    isDoNotDisturbGranted = isDoNotDisturbGranted,
                    onGrantDoNotDisturbAccess = onGrantDoNotDisturbAccess
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // The dots are the whole progress report; a "3 / 5" beside them
                // was the same fact in words, and words win the attention.
                Dots(count = steps.size, current = pagerState.currentPage)

                Spacer(Modifier.weight(1f))

                val last = pagerState.currentPage == steps.lastIndex
                HudButton(
                    label = if (last) {
                        stringResource(R.string.howto_done)
                    } else {
                        stringResource(R.string.howto_next)
                    },
                    enabled = true,
                    onClick = {
                        if (last) {
                            onBack()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier.width(NEXT_WIDTH).height(NEXT_HEIGHT)
                )
            }
        }
    }
}

/**
 * One step: its number set large and dim beside the words, so the sequence is
 * readable at a glance without a numeral competing with the instruction.
 */
@Composable
private fun StepPage(
    step: Step,
    isDoNotDisturbGranted: Boolean,
    onGrantDoNotDisturbAccess: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The opening card is not a step, it is what the steps are for.
        // Numbering it would push "plug in a cable" to two.
        if (step.number != null) {
            Text(
                text = step.number.toString(),
                style = ComboStyle,
                color = AccentDeep,
                modifier = Modifier.padding(end = 32.dp)
            )
        }
        Column(Modifier.widthIn(max = PROSE_MAX_WIDTH)) {
            Text(
                text = stringResource(step.titleId),
                style = if (step.number == null) DisplayStyle else ReadoutStyle,
                color = if (step.number == null) Bone else Accent
            )
            Text(
                text = stringResource(step.bodyId),
                style = BodyStyle,
                color = if (step.number == null) Steel else Bone,
                modifier = Modifier.padding(top = 12.dp)
            )
            if (step.asksForNotificationAccess) {
                DoNotDisturbAction(
                    granted = isDoNotDisturbGranted,
                    onGrant = onGrantDoNotDisturbAccess,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
        }
    }
}

/** The grant, offered in place. Once granted, the button becomes a statement. */
@Composable
private fun DoNotDisturbAction(
    granted: Boolean,
    onGrant: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (granted) {
        Text(
            text = stringResource(R.string.howto_allowed),
            style = LabelStyle,
            color = Accent,
            modifier = modifier
        )
    } else {
        HudButton(
            label = stringResource(R.string.howto_allow),
            enabled = true,
            onClick = onGrant,
            accent = AccentDeep,
            labelStyle = ButtonSmallStyle,
            prominent = false,
            labelColor = Steel,
            modifier = modifier.width(ALLOW_WIDTH).height(ALLOW_HEIGHT)
        )
    }
}

/** Progress dots: shape and colour both change, so colour alone is never it. */
@Composable
private fun Dots(count: Int, current: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until count) {
            Box(
                Modifier
                    .size(
                        width = if (i == current) DOT_ACTIVE_WIDTH else DOT_SIZE,
                        height = DOT_SIZE
                    )
                    .background(if (i == current) Accent else Steel.copy(alpha = 0.4f))
            )
        }
    }
}

private class Step(
    val number: Int?,
    val titleId: Int,
    val bodyId: Int,
    val asksForNotificationAccess: Boolean = false
)

private val STEPS = listOf(
    Step(null, R.string.howto_intro_title, R.string.howto_intro_body),
    Step(1, R.string.howto_step_1_title, R.string.howto_step_1_body),
    Step(2, R.string.howto_step_2_title, R.string.howto_step_2_body),
    Step(3, R.string.howto_step_3_title, R.string.howto_step_3_body),
    Step(
        number = 4,
        titleId = R.string.howto_step_4_title,
        bodyId = R.string.howto_step_4_body,
        asksForNotificationAccess = true
    ),
    Step(5, R.string.howto_step_5_title, R.string.howto_step_5_body)
)

private val SCREEN_PADDING = 40.dp
private val TOP_BAR_HEIGHT = 48.dp
private val PAGE_SPACING = 32.dp
private val PROSE_MAX_WIDTH = 620.dp
private val NEXT_WIDTH = 220.dp
private val NEXT_HEIGHT = 64.dp
private val ALLOW_WIDTH = 160.dp
private val ALLOW_HEIGHT = 48.dp
private val DOT_SIZE = 8.dp
private val DOT_ACTIVE_WIDTH = 28.dp
