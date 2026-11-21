package com.c0d3h01.androidtweaker.battery

import java.util.Locale

fun formatDuration(ms: Long?): String {
  if (ms == null || ms < 0) return "—"
  val totalMinutes = ms / 60_000
  val hours = totalMinutes / 60
  val minutes = totalMinutes % 60
  return when {
    hours >= 24 -> String.format(Locale.US, "%dd\u00A0%dh", hours / 24, hours % 24)
    hours > 0 -> String.format(Locale.US, "%dh\u00A0%02dm", hours, minutes)
    else -> "${minutes}m"
  }
}

fun formatMah(mah: Int?): String = mah?.let { "$it\u00A0mAh" } ?: "—"

fun formatPowerValue(w: Float?): String = w?.let {
  val precision = when {
    it > 0f && it < 0.01f -> "%.3f"
    it > 0f && it < 0.1f -> "%.2f"
    else -> "%.1f"
  }
  String.format(Locale.US, precision, it)
} ?: "—"

fun formatPower(w: Float?): String = w?.let { "${formatPowerValue(it)}\u00A0W" } ?: "—"

fun formatCurrent(ma: Int?): String = ma?.let { (if (it > 0) "+" else "") + String.format(Locale.US, "%d\u00A0mA", it) } ?: "—"

fun formatVoltage(mv: Int?): String = mv?.let { String.format(Locale.US, "%.2f\u00A0V", it / 1000f) } ?: "—"

fun formatTemp(c: Float?): String = c?.let { String.format(Locale.US, "%.1f\u00A0°C", it) } ?: "—"

fun chargeSpeedLabel(
  currentMa: Int?,
  powerW: Float?,
  plugged: Plugged,
): String =
  when {
    plugged == Plugged.NONE -> "Not charging"
    (powerW ?: 0f) >= 15f || (currentMa ?: 0) >= 3_000 -> "Super fast"
    (powerW ?: 0f) >= 7f || (currentMa ?: 0) >= 1_500 -> "Fast"
    (powerW ?: 0f) >= 2f || (currentMa ?: 0) >= 500 -> "Standard"
    else -> "Slow"
  }

fun plugLabel(plugged: Plugged): String =
  when (plugged) {
    Plugged.AC -> "AC charger"
    Plugged.USB -> "USB"
    Plugged.WIRELESS -> "Wireless charger"
    Plugged.DOCK -> "Dock"
    Plugged.NONE -> "Battery"
  }
