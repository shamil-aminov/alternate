package sh.aminov.alternate.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * The app lives in one dark theme on purpose: this is a cockpit, not a
 * document. Dynamic system colour is off — it would break the one place where
 * colour carries meaning, the tempo gauge.
 */
private val CockpitColors = darkColorScheme(
    primary = Accent,
    onPrimary = Void,
    secondary = AccentDeep,
    tertiary = Steel,
    background = Void,
    onBackground = Bone,
    surface = Ink,
    onSurface = Bone,
    outline = AccentDeep
)

/**
 * The font scale is pinned to 1.
 *
 * Everywhere else this would be wrong — a system-wide text size is an
 * accessibility setting and apps have no business overriding it. Here the
 * screen is an instrument panel whose numbers are already the largest type in
 * the app, laid out against fixed geometry: a 1.3× scale pushes the BPM
 * readout into the gauge and the chain counter off the edge, which helps
 * nobody read anything. The settings screen keeps ordinary body text at a
 * size that does not need scaling to be legible.
 */
@Composable
fun AlternateTheme(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val fixedScale = Density(density = density.density, fontScale = 1f)

    CompositionLocalProvider(LocalDensity provides fixedScale) {
        MaterialTheme(
            colorScheme = CockpitColors,
            content = content
        )
    }
}
