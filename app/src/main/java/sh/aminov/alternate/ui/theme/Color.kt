package sh.aminov.alternate.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Monochromatic palette: one blue and its lightness levels.
 *
 * The left and right lanes are told apart by position — the wave goes left or
 * right — not by colour. Colour is kept free for one job: on the gauge it
 * carries speed, running from white at rest to blue at full tilt.
 *
 * The one colour outside the scheme is [ZoneRed], and only for errors.
 */

/** Background. True black, for OLED and for depth. */
val Void = Color(0xFF000000)

/** Panel fills. Also black, to sit on the background without a seam. */
val Ink = Color(0xFF000000)

/** The blue: everything that glows. Also the fast end of the gauge. */
val Accent = Color(0xFF00D9F2)

/** Deep blue: borders and inactive states. */
val AccentDeep = Color(0xFF0B6C82)

/** Brightness peaks — white with a blue cast. The slow end of the gauge. */
val Bone = Color(0xFFDFF4FF)

/** Labels: grey pulled towards the same blue. */
val Steel = Color(0xFF5F7887)

/** Errors, and only errors. */
val ZoneRed = Color(0xFFFF3B3B)
