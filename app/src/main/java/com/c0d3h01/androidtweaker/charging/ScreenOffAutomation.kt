package com.c0d3h01.androidtweaker.charging

import android.content.Context
import com.c0d3h01.androidtweaker.access.AccessManager
import com.c0d3h01.androidtweaker.access.CommandRunner
import com.c0d3h01.androidtweaker.battery.BatteryReading
import com.c0d3h01.androidtweaker.engine.TweakEngine
import com.c0d3h01.androidtweaker.tuning.TuningSettings

/** Opted-in screen transitions; separate from charge holds and restoration. */
object ScreenOffAutomation {
  private var dozeApplied = false
  private var powerSaveApplied = false
  private var dataSaverApplied = false

  suspend fun tick(context: Context, reading: BatteryReading, previous: BatteryReading?, features: TuningSettings) {
    val changed = previous != null && previous.screenOn != reading.screenOn
    when {
      reading.screenOn && (dozeApplied || powerSaveApplied || dataSaverApplied || (changed && features.forceDoze)) -> {
        val runner = AccessManager.runnerOrNull() ?: return
        screenOn(context, runner, features)
      }

      !reading.screenOn && changed &&
        (features.forceDoze || features.killBackground || features.autoPowerSave || features.autoDataSaver) -> {
        val runner = AccessManager.runnerOrNull() ?: return
        screenOff(context, runner, features)
      }
    }
  }

  private suspend fun screenOff(
    context: Context,
    runner: CommandRunner,
    features: TuningSettings,
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
    runner: CommandRunner,
    features: TuningSettings,
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
