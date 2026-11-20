package com.c0d3h01.androidtweaker.battery

/** One immutable snapshot for the visible battery pages. */
data class BatteryUiState(
  val reading: BatteryReading? = null,
  val settings: BatterySettings = BatterySettings(),
  val sessions: List<BatterySession> = emptyList(),
  val samples: List<BatterySample> = emptyList(),
  val apps: List<AppUsage> = emptyList(),
  val usageAccess: Boolean = false,
)
