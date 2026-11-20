package com.c0d3h01.androidtweaker.data

import android.content.Context
import android.os.SystemClock
import com.c0d3h01.androidtweaker.util.ShellAccess
import java.io.File

/**
 * Runs opted-in charging controls and screen transitions from the monitor.
 */
object FeatureEnforcer {
  private var lastChargeAt: Long? = null
  private var dozeApplied = false
  private var powerSaveApplied = false
  private var dataSaverApplied = false

  fun ownsChargeHold(context: Context): Boolean =
    File(TweakEngine.directory(context), "custom/charge.restore").isFile

  suspend fun releaseCharging(context: Context): Boolean {
    lastChargeAt = null
    if (!ownsChargeHold(context)) return true
    val runner = ShellAccess.runnerOrNull(requireRoot = true) ?: return false
    return TweakEngine.command(context, runner, "charge reset").ok
  }

  suspend fun tick(
    context: Context,
    reading: BatteryReading,
    previous: BatteryReading?,
    features: Features,
  ) {
    val now = SystemClock.elapsedRealtime()
    val pluggedIn = reading.isPlugged && previous?.isPlugged != true
    // A charging control can make a connected charger disappear from the reported plug state.
    val batteryChanged = previous != null && (reading.level != previous.level ||
      (features.tempFuse > 0 && reading.tempC?.toInt() != previous.tempC?.toInt()))
    if (!features.chargingActive && ownsChargeHold(context)) {
      releaseCharging(context)
    } else if (features.chargingActive && (reading.isPlugged || batteryChanged || ownsChargeHold(context)) &&
      (pluggedIn || lastChargeAt?.let { now - it >= CHARGE_CONTROL_INTERVAL_MS } != false)
    ) {
      ShellAccess.runnerOrNull(requireRoot = true)?.let { runner ->
        lastChargeAt = now
        TweakEngine.command(context, runner, "charge")
      }
    }

    val changed = previous != null && previous.screenOn != reading.screenOn
    when {
      reading.screenOn && (dozeApplied || powerSaveApplied || dataSaverApplied || (changed && features.forceDoze)) -> {
        val runner = ShellAccess.runnerOrNull() ?: return
        screenOn(context, runner, features)
      }

      !reading.screenOn && changed &&
        (features.forceDoze || features.killBackground || features.autoPowerSave || features.autoDataSaver) -> {
        val runner = ShellAccess.runnerOrNull() ?: return
        screenOff(context, runner, features)
      }
    }
  }

  private suspend fun screenOff(
    context: Context,
    runner: ShellRunner,
    features: Features,
  ) {
    if (features.forceDoze) dozeApplied = TweakEngine.command(context, runner, "doze force").ok
    if (features.killBackground) TweakEngine.command(context, runner, "bg-kill")
    if (features.autoPowerSave && !powerSaveApplied) {
      powerSaveApplied = TweakEngine.command(context, runner, "power-save on").ok
    }
    if (features.autoDataSaver && !dataSaverApplied) {
      dataSaverApplied = TweakEngine.command(context, runner, "datasaver on").ok
    }
  }

  private suspend fun screenOn(
    context: Context,
    runner: ShellRunner,
    features: Features,
  ) {
    if (dozeApplied || features.forceDoze) {
      dozeApplied = !TweakEngine.command(context, runner, "doze unforce").ok
    }
    if (powerSaveApplied) {
      powerSaveApplied = !TweakEngine.command(context, runner, "power-save off").ok
    }
    if (dataSaverApplied) {
      dataSaverApplied = !TweakEngine.command(context, runner, "datasaver off").ok
    }
  }
}
