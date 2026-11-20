package com.c0d3h01.androidtweaker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.c0d3h01.androidtweaker.R

/**
 * Shared dark surfaces, borders, and signal colors for the app.
 */
object Bench {
  val Canvas = Color(0xFF06090B)
  val Panel = Color(0xFF11171A)
  val PanelRaised = Color(0xFF171E22)
  val Sunken = Color(0xFF0B1013)
  val Hairline = Color(0xFF232A2F)
  val HairlineStrong = Color(0xFF333D44)

  val Ink = Color(0xFFF2F5F4)
  val InkMuted = Color(0xFFA7B0B4)
  val InkFaint = Color(0xFF899398)

  val Signal = Color(0xFF3DE08B)
  val SignalSoft = Color(0xFF10281C)
  val Mint = Color(0xFF3DDC84)
  val Amber = Color(0xFFF2C14E)
  val Rose = Color(0xFFFF6B6B)
  val Sky = Color(0xFF8BD5FF)
  val Lavender = Color(0xFFC9B8F5)

  val GlassFill = Color(0x14FFFFFF)
  val GlassBorder = Color(0x1FFFFFFF)

  val CardRadius = 24.dp
  val InnerRadius = 16.dp
  val ChipRadius = 50.dp
  val Gutter = 20.dp

  val BarClearance = 20.dp
}

/**
 * One bundled sans (Inter) carries every label, heading and numeral, so the app
 * reads as a single system. JetBrains Mono is used for log output only, where
 * character alignment is the whole point. Nothing depends on a system font.
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun variableFont(
  resId: Int,
  weight: Int,
) = Font(
  resId = resId,
  weight = FontWeight(weight),
  variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val Inter =
  FontFamily(
    variableFont(R.font.inter, 400),
    variableFont(R.font.inter, 500),
    variableFont(R.font.inter, 600),
    variableFont(R.font.inter, 700),
  )

private val JetBrainsMono =
  FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
  )

private val NoFontPadding = PlatformTextStyle(includeFontPadding = false)

private fun display(
  size: Int,
  weight: FontWeight = FontWeight.SemiBold,
  line: Int = size + 8,
  tracking: Double = -0.02,
) = TextStyle(
  fontFamily = Inter,
  fontWeight = weight,
  fontSize = size.sp,
  lineHeight = line.sp,
  letterSpacing = tracking.em,
  platformStyle = NoFontPadding,
  fontFeatureSettings = "tnum",
)

private fun mono(
  size: Int,
  weight: FontWeight = FontWeight.Normal,
  line: Int = size + 4,
  tracking: Double = 0.0,
) = TextStyle(
  fontFamily = JetBrainsMono,
  fontWeight = weight,
  fontSize = size.sp,
  lineHeight = line.sp,
  letterSpacing = tracking.em,
  platformStyle = NoFontPadding,
)

private fun body(
  size: Int,
  weight: FontWeight = FontWeight.Normal,
  line: Int = size + 8,
  tracking: Double = 0.0,
) = TextStyle(
  fontFamily = Inter,
  fontWeight = weight,
  fontSize = size.sp,
  lineHeight = line.sp,
  letterSpacing = tracking.em,
  platformStyle = NoFontPadding,
  fontFeatureSettings = "tnum",
)

private val Type =
  Typography(
    displayLarge = display(44, FontWeight.Bold, 50, -0.035),
    displayMedium = display(34, FontWeight.Bold, 40, -0.03),
    headlineLarge = display(29, FontWeight.SemiBold, 35, -0.025),
    headlineMedium = display(23, FontWeight.SemiBold, 28, -0.02),
    headlineSmall = display(19, FontWeight.SemiBold, 24, -0.012),
    titleLarge = display(17, FontWeight.SemiBold, 23, -0.01),
    titleMedium = body(15, FontWeight.SemiBold, 20),
    bodyLarge = body(16, FontWeight.Normal, 24),
    bodyMedium = body(14, FontWeight.Normal, 20, 0.005),
    bodySmall = body(12, FontWeight.Normal, 16, 0.01),
    labelLarge = body(13, FontWeight.SemiBold, 18, 0.02),
    labelMedium = body(11, FontWeight.Medium, 14, 0.04),
    labelSmall = body(10, FontWeight.Medium, 12, 0.06),
  )

/** Extra styles that Material's scale does not cover. */
object AppType {
  /** Uppercase section label above a card or screen title. */
  val section = body(11, FontWeight.SemiBold, 16, 0.09)
  val micro = body(10, FontWeight.Medium, 13, 0.07)
  val mono = body(11, FontWeight.Normal, 16)
  val monoStrong = body(11, FontWeight.SemiBold, 16)
  val data = display(28, FontWeight.Bold, 30, -0.025).copy(fontFeatureSettings = "tnum")
  val dataSmall = display(21, FontWeight.Bold, 24, -0.02).copy(fontFeatureSettings = "tnum")
  val hero = display(42, FontWeight.Bold, 46, -0.035)
  val log = mono(10, FontWeight.Normal, 15)
}

private val Scheme =
  darkColorScheme(
    background = Bench.Canvas,
    surface = Bench.Panel,
    surfaceVariant = Bench.PanelRaised,
    primary = Bench.Signal,
    onPrimary = Color(0xFF04170E),
    primaryContainer = Bench.SignalSoft,
    onPrimaryContainer = Bench.Ink,
    secondary = Bench.Mint,
    onSecondary = Color(0xFF06140D),
    tertiary = Bench.Sky,
    onTertiary = Color(0xFF071522),
    error = Bench.Rose,
    onError = Color(0xFF24090B),
    onBackground = Bench.Ink,
    onSurface = Bench.Ink,
    onSurfaceVariant = Bench.InkMuted,
    outline = Bench.HairlineStrong,
    outlineVariant = Bench.Hairline,
  )

@Composable
fun TweakerTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = Scheme, typography = Type, content = content)
}
