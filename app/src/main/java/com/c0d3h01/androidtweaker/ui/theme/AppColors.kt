package com.c0d3h01.androidtweaker.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shared dark surfaces, borders, and signal colors for the app.
 */
object AppColors {
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

internal val AppColorScheme =
  darkColorScheme(
    background = AppColors.Canvas,
    surface = AppColors.Panel,
    surfaceVariant = AppColors.PanelRaised,
    primary = AppColors.Signal,
    onPrimary = Color(0xFF04170E),
    primaryContainer = AppColors.SignalSoft,
    onPrimaryContainer = AppColors.Ink,
    secondary = AppColors.Mint,
    onSecondary = Color(0xFF06140D),
    tertiary = AppColors.Sky,
    onTertiary = Color(0xFF071522),
    error = AppColors.Rose,
    onError = Color(0xFF24090B),
    onBackground = AppColors.Ink,
    onSurface = AppColors.Ink,
    onSurfaceVariant = AppColors.InkMuted,
    outline = AppColors.HairlineStrong,
    outlineVariant = AppColors.Hairline,
  )
