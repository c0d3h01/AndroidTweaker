package com.c0d3h01.androidtweaker.battery

/** Settings shown on the Battery screen; every alarm can be disabled on its own. */
data class BatterySettings(
  val monitorEnabled: Boolean = false,
  val refreshSeconds: Int = 5,
  val iconStyle: Int = ICON_PERCENT_TEMP,
  val notifyFull: Boolean = true,
  val notifyLow: Boolean = true,
  val lowLevel: Int = 15,
  val notifyLevel: Boolean = false,
  val levelAlarm: Int = 80,
  val notifyHighTemp: Boolean = true,
  val highTempC: Int = 45,
  val notifyLowTemp: Boolean = false,
  val lowTempC: Int = 5,
  val chargingSummary: Boolean = true,
  val overnightReport: Boolean = true,
  val weeklySummary: Boolean = true,
  val anomalyAlert: Boolean = false,
) {
  companion object {
    const val ICON_OFF = 0
    const val ICON_PERCENT = 1
    const val ICON_PERCENT_TEMP = 2
    const val ICON_RING = 3
  }
}
