package com.c0d3h01.androidtweaker.data

internal const val CHARGE_CONTROL_INTERVAL_MS = 20_000L

internal fun monitorRefreshDelayMs(
  refreshSeconds: Int,
  screenOn: Boolean,
  plugged: Boolean,
  charging: Boolean,
  chargeControl: Boolean,
  needsIdleSample: Boolean,
): Long? {
  val activeDelay = refreshSeconds.coerceIn(15, 60) * 1_000L
  return when {
    plugged && chargeControl -> if (screenOn) minOf(activeDelay, CHARGE_CONTROL_INTERVAL_MS) else CHARGE_CONTROL_INTERVAL_MS
    screenOn -> activeDelay
    charging -> 30_000L
    needsIdleSample -> 5 * 60_000L
    else -> null
  }
}
