package com.c0d3h01.androidtweaker.profiles

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.c0d3h01.androidtweaker.R
import com.c0d3h01.androidtweaker.ui.theme.AppColors

data class ProfileSpec(
  val profile: Profile,
  @StringRes val title: Int,
  @StringRes val desc: Int,
  val level: Int,
  val tag: String,
  val tone: Color,
)

val PROFILE_SPECS =
  listOf(
    ProfileSpec(Profile.BATTERY, R.string.prof_battery, R.string.pdesc_battery, 1, "ENDURANCE", AppColors.Mint),
    ProfileSpec(Profile.BALANCED, R.string.prof_balanced, R.string.pdesc_balanced, 2, "DAILY", AppColors.Sky),
    ProfileSpec(Profile.PERFORMANCE, R.string.prof_performance, R.string.pdesc_performance, 3, "RESPONSE", AppColors.Signal),
    ProfileSpec(Profile.GAMING, R.string.prof_gaming, R.string.pdesc_gaming, 4, "MAX OUTPUT", AppColors.Amber),
  )
