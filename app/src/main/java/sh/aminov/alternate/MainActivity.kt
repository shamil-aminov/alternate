package sh.aminov.alternate

import android.content.ActivityNotFoundException
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import sh.aminov.alternate.ui.MidiViewModel
import sh.aminov.alternate.ui.screens.ConfigScreen
import sh.aminov.alternate.ui.screens.InstructionsScreen
import sh.aminov.alternate.ui.screens.KeypadScreen
import sh.aminov.alternate.ui.screens.SettingsScreen
import sh.aminov.alternate.ui.theme.AlternateTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MidiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestHighestRefreshRate()
        drawEdgeToEdge()

        setContent {
            AlternateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Read once. If it changed mid-session the walkthrough
                    // would try to become the start destination underneath a
                    // running back stack.
                    val startsOnboarding = remember { !viewModel.settings.value.hasSeenOnboarding }
                    val uiState by viewModel.uiState.collectAsState()
                    val settings by viewModel.settings.collectAsState()
                    val dndGranted by viewModel.isDoNotDisturbGranted.collectAsState()

                    // System bars are governed once, for the whole app, rather
                    // than per screen. A menu that shows a status bar and a pad
                    // that does not is two different apps as far as the eye is
                    // concerned, and the edge swipe the bars invite is just as
                    // unwelcome while picking a device as while streaming.
                    LaunchedEffect(settings.hideSystemBars) {
                        setSystemBarsHidden(settings.hideSystemBars)
                    }

                    LaunchedEffect(settings.keepScreenOn) {
                        setKeepScreenOn(settings.keepScreenOn)
                    }

                    // A plain cross-fade. Screens used to assemble themselves
                    // piece by piece from the edges, which looked deliberate
                    // in isolation and arbitrary in use: the order the pieces
                    // arrived in had no relation to the order anyone reads
                    // them, so every screen change asked you to watch a
                    // sequence that meant nothing. A fade says "this is a
                    // different screen now" and gets out of the way.
                    NavHost(
                        navController = navController,
                        startDestination = if (startsOnboarding) ROUTE_ONBOARDING else ROUTE_CONFIG,
                        enterTransition = { fadeIn(tween(ENTER_MS, easing = LinearOutSlowInEasing)) },
                        exitTransition = { fadeOut(tween(EXIT_MS, easing = FastOutLinearInEasing)) },
                        popEnterTransition = {
                            fadeIn(tween(ENTER_MS, easing = LinearOutSlowInEasing))
                        },
                        popExitTransition = {
                            fadeOut(tween(EXIT_MS, easing = FastOutLinearInEasing))
                        }
                    ) {
                        composable(ROUTE_CONFIG) {
                            LaunchedEffect(Unit) {

                                viewModel.releaseDoNotDisturb()
                            }
                            ConfigScreen(
                                state = uiState,
                                onStartClick = {
                                    viewModel.startKeypad {
                                        navController.navigate(keypadRoute(demo = false))
                                    }
                                },
                                onDemoClick = {
                                    navController.navigate(keypadRoute(demo = true))
                                },
                                onDeviceSelected = viewModel::selectDevice,
                                onSettingsClick = { navController.navigate(ROUTE_SETTINGS) },
                                onHowItWorksClick = { navController.navigate(ROUTE_HOW_TO) }
                            )
                        }

                        composable(ROUTE_SETTINGS) {
                            SettingsScreen(
                                settings = settings,
                                isDoNotDisturbGranted = dndGranted,
                                onSettingsChange = viewModel::updateSettings,
                                onRequestDoNotDisturbAccess = ::openDoNotDisturbAccess,
                                onReset = viewModel::resetSettings,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_ONBOARDING) {
                            InstructionsScreen(
                                isOnboarding = true,
                                isDoNotDisturbGranted = dndGranted,
                                onGrantDoNotDisturbAccess = ::openDoNotDisturbAccess,
                                onBack = {
                                    viewModel.markOnboardingSeen()
                                    navController.navigate(ROUTE_CONFIG) {
                                        popUpTo(ROUTE_ONBOARDING) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(ROUTE_HOW_TO) {
                            InstructionsScreen(onBack = { navController.popBackStack() })
                        }

                        composable(
                            route = ROUTE_KEYPAD,
                            arguments = listOf(
                                navArgument(ARG_DEMO) {
                                    type = NavType.BoolType
                                    defaultValue = false
                                }
                            )
                        ) { entry ->
                            val isDemo = entry.arguments?.getBoolean(ARG_DEMO) == true

                            LaunchedEffect(Unit) { viewModel.engageDoNotDisturb() }

                            KeypadScreen(
                                tuning = settings.tuning,
                                liteGraphics = settings.liteGraphics,
                                showDiagnostics = settings.showDiagnostics,
                                isDemo = isDemo,
                                onNoteOn = { note -> viewModel.sendNote(note, isPressed = true) },
                                onNoteOff = { note -> viewModel.sendNote(note, isPressed = false) },
                                onExit = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * A heads-up notification lands on the pad without stopping the activity,
     * so `onStop` never runs and a held note would stay down in the game. The
     * screen releases its own notes; this silences the whole channel.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) viewModel.panic()
    }

    /** The policy grant may have been given while we were away. */
    override fun onResume() {
        super.onResume()
        viewModel.refreshDoNotDisturbAccess()
    }

    /** Minimised with a finger down: kill the notes or the game keeps them. */
    override fun onStop() {
        super.onStop()
        viewModel.panic()
        viewModel.releaseDoNotDisturb()
    }

    private fun openDoNotDisturbAccess() {
        try {
            startActivity(viewModel.doNotDisturbSettingsIntent())
        } catch (_: ActivityNotFoundException) {
            // Some vendor builds ship without the policy-access screen. The
            // toggle simply stays disabled, which is what it already shows.
        }
    }

    private fun setKeepScreenOn(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /**
     * The screen draws under the system bars and the bars go transparent: a
     * light navigation strip across a dark cockpit would break the picture.
     */
    @Suppress("DEPRECATION")
    private fun drawEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }

    /**
     * The fastest display mode at the current resolution: more frames is a
     * shorter path from touch to picture, and a sensor polled more often.
     */
    private fun requestHighestRefreshRate() {
        val display = currentDisplay() ?: return
        val currentMode = display.mode ?: return
        val fastest = display.supportedModes
            .filter {
                it.physicalWidth == currentMode.physicalWidth &&
                    it.physicalHeight == currentMode.physicalHeight
            }
            .maxByOrNull { it.refreshRate } ?: return

        window.attributes = window.attributes.apply { preferredDisplayModeId = fastest.modeId }
    }

    @Suppress("DEPRECATION")
    private fun currentDisplay(): Display? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else windowManager.defaultDisplay

    /**
     * A stray swipe at the screen edge mid-run costs more than a hidden clock.
     */
    private fun setSystemBarsHidden(hidden: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (hidden) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private companion object {
        const val ROUTE_CONFIG = "config"
        const val ROUTE_SETTINGS = "settings"
        const val ROUTE_HOW_TO = "howto"
        const val ROUTE_ONBOARDING = "onboarding"

        /** The pad is one screen whether or not a port is open. */
        const val ARG_DEMO = "demo"
        const val ROUTE_KEYPAD = "keypad?$ARG_DEMO={$ARG_DEMO}"

        fun keypadRoute(demo: Boolean) = "keypad?$ARG_DEMO=$demo"

        /** Leaving is quicker than arriving. */
        const val ENTER_MS = 170
        const val EXIT_MS = 110

    }
}
