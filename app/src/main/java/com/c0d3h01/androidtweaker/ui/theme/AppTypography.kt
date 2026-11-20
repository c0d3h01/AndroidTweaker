package com.c0d3h01.androidtweaker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.c0d3h01.androidtweaker.R

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

internal val AppTypography =
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
