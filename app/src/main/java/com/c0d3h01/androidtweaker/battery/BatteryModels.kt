package com.c0d3h01.androidtweaker.battery


enum class Plugged { NONE, AC, USB, WIRELESS, DOCK }

enum class BatteryStatus { CHARGING, DISCHARGING, FULL, IDLE, UNKNOWN }

/** One live reading, normalised across the many ways Android reports battery data. */
data class BatteryReading(
  val at: Long,
  val level: Int?,
  val status: BatteryStatus,
  val plugged: Plugged,
  val currentMa: Int?,
  val voltageMv: Int?,
  val tempC: Float?,
  val chargeCounterUah: Long?,
  val designMah: Int?,
  val fullMah: Int?,
  val cycles: Int?,
  val health: String?,
  val screenOn: Boolean,
) {
  val isPlugged: Boolean get() = plugged != Plugged.NONE
  val isCharging: Boolean get() = status == BatteryStatus.CHARGING
  val powerW: Float? get() = batteryPowerWatts(currentMa, voltageMv)
  val capacityMah: Int? get() = fullMah?.takeIf { it > 0 } ?: designMah?.takeIf { it > 0 }
}

/** App usage for the current discharging session, when Usage access is granted. */
data class AppUsage(
  val packageName: String,
  val label: String,
  val screenMs: Long,
  val launches: Int,
  val share: Float,
)

data class HealthEstimate(
  val capacityMah: Int?,
  val designMah: Int?,
  val percent: Int?,
  val confidence: String,
  val sessionsUsed: Int,
  val cycles: Int?,
) {
  val label: String
    get() =
      when {
        percent == null -> "Not yet"
        percent >= 90 -> "Excellent"
        percent >= 80 -> "Good"
        percent >= 70 -> "OK"
        percent >= 60 -> "Fair"
        else -> "Bad"
      }
}

data class BatterySession(
  val start: Long,
  val end: Long,
  val charging: Boolean,
  val startLevel: Int,
  val endLevel: Int,
  val addedMah: Int,
  val peakPowerW: Float,
  val peakTempC: Float?,
  val charger: Plugged,
  val quality: Int,
  val capacityMah: Int?,
  val screenOnMs: Long,
  val screenOffMs: Long,
  val deepSleepMs: Long,
) {
  val durationMs: Long get() = (end - start).coerceAtLeast(0)
  val levelDelta: Int get() = (endLevel - startLevel)
}

data class ScreenStats(
  val screenOnMs: Long,
  val screenOffMs: Long,
  val awakeMs: Long,
  val deepSleepMs: Long,
)

data class BatterySample(
  val at: Long,
  val level: Int,
  val tempC: Float?,
  val currentMa: Int?,
  val voltageMv: Int?,
  val charging: Boolean,
  val screenOn: Boolean,
)
