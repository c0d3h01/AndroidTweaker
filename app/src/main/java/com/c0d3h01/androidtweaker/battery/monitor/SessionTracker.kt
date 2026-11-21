package com.c0d3h01.androidtweaker.battery.monitor

import com.c0d3h01.androidtweaker.battery.BatteryReading
import com.c0d3h01.androidtweaker.battery.BatterySession
import com.c0d3h01.androidtweaker.battery.BatteryStatus
import com.c0d3h01.androidtweaker.battery.analysis.chargeQuality
import kotlin.math.abs
import kotlin.math.roundToInt

/** In-memory session transitions. Clock values are supplied by the Android owner. */
class SessionTracker {
  private var charge: SessionBuilder? = null
  private var discharge: SessionBuilder? = null
  var startedCharge: Boolean = false
    private set

  fun onReading(reading: BatteryReading, elapsedMs: Long, deepSleepMs: Long, nowMs: Long = reading.at): List<BatterySession> {
    startedCharge = false
    val level = reading.level ?: return emptyList()
    val completed = mutableListOf<BatterySession>()
    when {
      reading.isCharging || (reading.isPlugged && reading.status == BatteryStatus.FULL) -> {
        if (charge == null) {
          charge = SessionBuilder(reading, true, elapsedMs, deepSleepMs)
          startedCharge = true
        } else {
          charge?.update(reading, chargingNow = reading.isCharging, elapsedMs, deepSleepMs)
        }
        if (discharge != null) {
          discharge?.update(reading, chargingNow = reading.isCharging, elapsedMs, deepSleepMs)
          discharge?.finish(level, charging = false, nowMs)?.let { completed.add(it) }
          discharge = null
        }
      }

      reading.isPlugged -> {
        if (discharge != null) {
          discharge?.update(reading, chargingNow = false, elapsedMs, deepSleepMs)
          discharge?.finish(level, charging = false, nowMs)?.let { completed.add(it) }
          discharge = null
        }
        charge?.update(reading, chargingNow = false, elapsedMs, deepSleepMs)
      }

      else -> {
        if (charge != null) {
          charge?.update(reading, chargingNow = false, elapsedMs, deepSleepMs)
          val session = charge?.finish(level, charging = true, nowMs)
          charge = null
          if (session != null) {
            completed.add(session)
          }
        }
        if (discharge == null) {
          discharge = SessionBuilder(reading, false, elapsedMs, deepSleepMs)
        } else {
          discharge?.update(reading, chargingNow = reading.isCharging, elapsedMs, deepSleepMs)
        }
      }
    }
    return completed
  }
}

/** Accumulates a charge or discharge session across monitor ticks. */
private class SessionBuilder(
  private val reading: BatteryReading,
  private val charging: Boolean,
  elapsedMs: Long,
  deepSleepMs: Long,
) {
  private val start = reading.at
  private val startLevel = reading.level ?: 0
  private var peakPowerW = reading.powerW ?: 0f
  private var peakTempC: Float? = reading.tempC
  private var addedMah = 0f
  private val startCounter = reading.chargeCounterUah
  private var lastCounter = reading.chargeCounterUah
  private var lastTick = elapsedMs
  private var lastDeep = deepSleepMs
  private var screenOnMs = 0L
  private var screenOffMs = 0L
  private var deepSleepMs = 0L
  private var lastCurrentMa = reading.currentMa
  private var lastScreenOn = reading.screenOn

  fun update(
    next: BatteryReading,
    chargingNow: Boolean,
    now: Long,
    deep: Long,
  ) {
    val dt = (now - lastTick).coerceAtLeast(0)
    val powerDt = dt.coerceAtMost(120_000)
    lastTick = now
    deepSleepMs += (deep - lastDeep).coerceAtLeast(0)
    lastDeep = deep
    if (lastScreenOn) screenOnMs += dt else screenOffMs += dt
    lastScreenOn = next.screenOn
    if (charging && chargingNow) {
      val ma = abs(next.currentMa ?: lastCurrentMa ?: 0)
      if (ma > 0) addedMah += ma * powerDt / 3_600_000f
    }
    lastCounter = next.chargeCounterUah
    lastCurrentMa = next.currentMa
    peakPowerW = maxOf(peakPowerW, next.powerW ?: 0f)
    if (next.tempC != null) peakTempC = maxOf(peakTempC ?: next.tempC, next.tempC)
  }

  fun finish(
    endLevel: Int,
    charging: Boolean,
    end: Long,
  ): BatterySession? {
    if (end - start < 45_000) return null
    val delta = (endLevel - startLevel).coerceAtLeast(0)
    val counterMah =
      if (startCounter != null && lastCounter != null) {
        (lastCounter!! - startCounter) / 1000f
      } else {
        0f
      }
    val added = counterMah.takeIf { it > 0f } ?: addedMah
    val capacity = if (charging && delta >= 15 && added > 0) (added / delta * 100f).roundToInt() else null
    return BatterySession(
      start = start,
      end = end,
      charging = charging,
      startLevel = startLevel,
      endLevel = endLevel,
      addedMah = added.roundToInt().coerceAtLeast(0),
      peakPowerW = peakPowerW,
      peakTempC = peakTempC,
      charger = reading.plugged,
      quality =
        if (charging) chargeQuality(startLevel, endLevel, peakTempC, peakPowerW) else 0,
      capacityMah = capacity?.takeIf { it in 500..20_000 },
      screenOnMs = screenOnMs,
      screenOffMs = screenOffMs,
      deepSleepMs = deepSleepMs,
    )
  }
}
