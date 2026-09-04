package sh.aminov.alternate.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.sp
import sh.aminov.alternate.R

/**
 * Tektur, a variable typeface: one file instead of four cuts. From Android 8
 * the system interpolates the weight axis; older versions get the regular cut.
 */
val Tektur = FontFamily(
    Font(R.font.tektur, FontWeight.Normal),
    Font(R.font.tektur, FontWeight.Medium),
    Font(R.font.tektur, FontWeight.Bold),
    Font(R.font.tektur, FontWeight.Black)
)

/**
 * One modular scale, roughly a factor of 1.4 per step: 12 18 24 32 44 68.
 *
 * Wide steps on purpose. A smooth scale reads like a table and a table has no
 * hierarchy; one glance should land on the main number. Nothing outside this
 * file invents a seventh size.
 */

/** Digits lean forward — the signature of an arcade display. */
private val Slant = TextGeometricTransform(skewX = -0.16f)

/** The chain counter. The only genuinely large number. */
val ComboStyle = TextStyle(
    fontFamily = Tektur,
    fontWeight = FontWeight.Black,
    fontSize = 68.sp,
    letterSpacing = 1.sp,
    textGeometricTransform = Slant
)

/** The wordmark on the connection screen. */
val DisplayStyle = TextStyle(
    fontFamily = Tektur,
    fontWeight = FontWeight.Black,
    fontSize = 44.sp,
    letterSpacing = 10.sp
)

/** Corner readouts. */
val ReadoutStyle = TextStyle(
    fontFamily = Tektur,
    fontWeight = FontWeight.Bold,
    fontSize = 32.sp,
    letterSpacing = 1.sp,
    textGeometricTransform = Slant
)

/** Values inside settings rows, and the stepper buttons. */
val ReadoutSmallStyle = TextStyle(
    fontFamily = Tektur,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    letterSpacing = 0.5.sp
)

/** Body copy in panels. */
val BodyStyle = TextStyle(
    fontFamily = Tektur,
    fontWeight = FontWeight.Medium,
    fontSize = 18.sp,
    letterSpacing = 0.5.sp
)

/**
 * The label above a value, and every caption. Wide tracking is what makes it
 * read as a label, dialled back to 4 sp so 12 sp glyphs do not pull apart.
 */
val LabelStyle = TextStyle(
    fontFamily = Tektur,
    fontWeight = FontWeight.Bold,
    fontSize = 12.sp,
    letterSpacing = 4.sp
)

/**
 * Explanatory text under a setting. Same size as [LabelStyle] and deliberately
 * untracked: tracking makes a word in caps a label, but breaks a sentence.
 */
val CaptionStyle = TextStyle(
    fontFamily = Tektur,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    letterSpacing = 0.2.sp
)

/** The label on a large button. Tracking does the shouting, not the size. */
val ButtonStyle = TextStyle(
    fontFamily = Tektur,
    fontWeight = FontWeight.Black,
    fontSize = 28.sp,
    letterSpacing = 10.sp
)


/** The back chevron, set in the typeface. No tracking: it is a lone glyph. */
val ChevronStyle = TextStyle(
    fontFamily = Tektur,
    fontWeight = FontWeight.Black,
    fontSize = 40.sp
)

/**
 * A button label inside a dialog. Screen-level [ButtonStyle] is sized for arm's
 * length; at the foot of a panel it leaves no air around the notched corners.
 */
val ButtonSmallStyle = TextStyle(
    fontFamily = Tektur,
    fontWeight = FontWeight.Bold,
    fontSize = 17.sp,
    letterSpacing = 4.sp
)
