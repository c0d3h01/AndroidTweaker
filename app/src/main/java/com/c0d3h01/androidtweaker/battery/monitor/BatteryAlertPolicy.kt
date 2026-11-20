package com.c0d3h01.androidtweaker.battery.monitor

import com.c0d3h01.androidtweaker.battery.BatteryReading
import com.c0d3h01.androidtweaker.battery.BatterySettings
import com.c0d3h01.androidtweaker.battery.BatteryStatus
import com.c0d3h01.androidtweaker.battery.formatTemp
import kotlin.math.abs

/** A notification request; Android delivery is owned by the notification renderer. */
data class BatteryAlert(val key: String, val title: String, val body: String)

/** Pure threshold and latch policy with explicitly supplied time. */
class BatteryAlertPolicy {
  private val lastAlarmAt = mutableMapOf<String, Long>()
  private var fullNotified = false
  private var lowNotified = false
  private var levelNotified: Int? = null

  fun onChargeStarted() { fullNotified = false }

  fun onLevelChange(reading: BatteryReading, previous: BatteryReading?) {
    val level = reading.level ?: return
    if (previous?.level != level) {
      if (level > (previous?.level ?: level)) lowNotified = false
      if (level < 100) fullNotified = false
      if (levelNotified != null && abs(level - levelNotified!!) >= 3) levelNotified = null
    }
  }

  fun evaluate(
    reading: BatteryReading,
    previous: BatteryReading?,
    settings: BatterySettings,
    now: Long,
  ): List<BatteryAlert> {
    val level = reading.level ?: return emptyList()
    val result = mutableListOf<BatteryAlert>()
    if (settings.notifyFull && !fullNotified &&
      (reading.status == BatteryStatus.FULL || (level >= 100 && reading.isPlugged))
    ) {
      fullNotified = true
      result.add(BatteryAlert("full", "Battery full", "$level% · unplug when convenient"))
    }
    if (settings.notifyLow && !lowNotified && !reading.isPlugged && level <= settings.lowLevel) {
      lowNotified = true
      result.add(BatteryAlert("low", "Battery low", "$level% left · plug in soon"))
    }
    if (settings.notifyLevel && levelNotified == null) {
      val crossedUp = reading.isPlugged && previous?.level != null && previous.level!! < settings.levelAlarm && level >= settings.levelAlarm
      val crossedDown =
        !reading.isPlugged && previous?.level != null && previous.level!! > settings.levelAlarm && level <= settings.levelAlarm
      if (crossedUp || crossedDown) {
        levelNotified = level
        result.add(BatteryAlert(
          "level",
          if (crossedUp) "Battery reached ${settings.levelAlarm}%" else "Battery dropped to ${settings.levelAlarm}%",
          if (crossedUp) "You can unplug now" else "$level% remaining",
        ))
      }
    }
    val temp = reading.tempC
    if (settings.notifyHighTemp && temp != null && temp >= settings.highTempC) {
      if (now - (lastAlarmAt["highTemp"] ?: 0) >= 30 * 60_000L) {
        lastAlarmAt["highTemp"] = now
        result.add(BatteryAlert("highTemp", "Battery is hot", formatTemp(temp) + " · above ${settings.highTempC}°C"))
      }
    }
    if (settings.notifyLowTemp && temp != null && temp <= settings.lowTempC) {
      if (now - (lastAlarmAt["lowTemp"] ?: 0) >= 30 * 60_000L) {
        lastAlarmAt["lowTemp"] = now
        result.add(BatteryAlert("lowTemp", "Battery is cold", formatTemp(temp) + " · below ${settings.lowTempC}°C"))
      }
    }
    return result
  }
}
