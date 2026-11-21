package com.c0d3h01.androidtweaker.charging

import android.content.Context
import android.os.SystemClock
import com.c0d3h01.androidtweaker.access.AccessManager
import com.c0d3h01.androidtweaker.battery.BatteryReading
import com.c0d3h01.androidtweaker.battery.monitor.CHARGE_CONTROL_INTERVAL_MS
import com.c0d3h01.androidtweaker.engine.EngineFiles
import com.c0d3h01.androidtweaker.engine.TweakEngine
import com.c0d3h01.androidtweaker.tuning.TuningSettings
import java.io.File

/**
 * Enforces and restores only the monitor-owned charging hold.
 */
object ChargingController {
  private var lastChargeAt: Long? = null

  fun ownsChargeHold(context: Context): Boolean =
    File(EngineFiles.directory(context), "custom/charge.restore").isFile

  suspend fun releaseCharging(context: Context): Boolean {
    lastChargeAt = null
    if (!ownsChargeHold(context)) return true
    val runner = AccessManager.runnerOrNull(requireRoot = true) ?: return false
    return TweakEngine.command(context, runner, "charge reset").ok
  }

  suspend fun tick(
    context: Context,
    reading: BatteryReading,
    previous: BatteryReading?,
    features: TuningSettings,
  ) {
    val now = SystemClock.elapsedRealtime()
    val pluggedIn = reading.isPlugged && previous?.isPlugged != true
    // A charging control can make a connected charger disappear from the reported plug state.
    val batteryChanged = previous != null && (reading.level != previous.level ||
      (features.charging.tempFuse > 0 && reading.tempC?.toInt() != previous.tempC?.toInt()))
    if (!features.charging.active && ownsChargeHold(context)) {
      releaseCharging(context)
    } else if (features.charging.active && (reading.isPlugged || batteryChanged || ownsChargeHold(context)) &&
      (pluggedIn || lastChargeAt?.let { now - it >= CHARGE_CONTROL_INTERVAL_MS } != false)
    ) {
      AccessManager.runnerOrNull(requireRoot = true)?.let { runner ->
        lastChargeAt = now
        TweakEngine.command(context, runner, "charge")
      }
    }

  }
}
