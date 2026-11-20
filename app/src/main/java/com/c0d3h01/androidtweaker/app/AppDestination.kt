package com.c0d3h01.androidtweaker.app

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import com.c0d3h01.androidtweaker.MainActivity
import com.c0d3h01.androidtweaker.R

internal enum class AppDestination(
  val key: String,
  @StringRes val title: Int,
  val icon: ImageVector? = null,
) {
  OVERVIEW(MainActivity.SCREEN_HOME, R.string.nav_overview, Icons.Outlined.Dashboard),
  BATTERY(MainActivity.SCREEN_BATTERY, R.string.nav_battery, Icons.Outlined.BatteryChargingFull),
  DEVICE(MainActivity.SCREEN_DEVICE, R.string.nav_device, Icons.Outlined.Devices),
  TUNING(MainActivity.SCREEN_TWEAKS, R.string.nav_tuning, Icons.Outlined.Tune),
  SETTINGS(MainActivity.SCREEN_SETTINGS, R.string.nav_settings, Icons.Outlined.Settings),
  PERFORMANCE(MainActivity.SCREEN_PERFORMANCE, R.string.nav_performance),
  HARDWARE(MainActivity.SCREEN_HARDWARE, R.string.nav_hardware),
  LOGS(MainActivity.SCREEN_LOGS, R.string.nav_logs),
  TOOLS(MainActivity.SCREEN_TOOLS, R.string.nav_diagnostics),
  ;

  val section: AppDestination
    get() = when (this) {
      HARDWARE, PERFORMANCE -> DEVICE
      LOGS, TOOLS -> SETTINGS
      else -> this
    }

  companion object {
    val sections = entries.filter { it.icon != null }

    fun fromKey(key: String?) = entries.firstOrNull { it.key == key }
  }
}
