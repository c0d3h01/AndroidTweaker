package com.c0d3h01.androidtweaker.tuning

import android.content.Context
import com.c0d3h01.androidtweaker.charging.ChargingSettings
import com.c0d3h01.androidtweaker.engine.EngineFiles
import java.io.File

/** Persists [TuningSettings] and writes the conf the engine sources. */
object TuningStore {
  private const val PREFS = "feature_switches"

  fun load(context: Context): TuningSettings {
    val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun set(key: String): Set<String> = p.getStringSet(key, emptySet()).orEmpty()
    return TuningSettings(
      charging = ChargingSettings(
        chargeLimit = p.getInt("chargeLimit", 0),
        chargeBypass = p.getBoolean("chargeBypass", false),
        tempFuse = p.getInt("tempFuse", 0),
      ),
      throttleOff = p.getBoolean("throttleOff", false),
      forceDoze = p.getBoolean("forceDoze", false),
      dozeWhitelist = set("dozeWhitelist"),
      autoDataSaver = p.getBoolean("autoDataSaver", false),
      autoPowerSave = p.getBoolean("autoPowerSave", false),
      killBackground = p.getBoolean("killBackground", false),
      disableLogs = p.getBoolean("disableLogs", false),
      phantomLimit = p.getInt("phantomLimit", 0),
      wifiSleep = p.getBoolean("wifiSleep", false),
      scanAlways = p.getBoolean("scanAlways", false),
      heavyApps = set("heavyApps"),
      lightApps = set("lightApps"),
      bloat = set("bloat"),
      resolution = Resolution.entries.firstOrNull { it.key == p.getString("resolution", null) } ?: Resolution.DEFAULT,
      peakFps = p.getInt("peakFps", 0),
    ).sanitized()
  }

  fun save(
    context: Context,
    features: TuningSettings,
  ) {
    val clean = features.sanitized()
    context
      .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
      .edit()
      .putInt("chargeLimit", clean.charging.chargeLimit)
      .putBoolean("chargeBypass", clean.charging.chargeBypass)
      .putInt("tempFuse", clean.charging.tempFuse)
      .putBoolean("throttleOff", clean.throttleOff)
      .putBoolean("forceDoze", clean.forceDoze)
      .putStringSet("dozeWhitelist", clean.dozeWhitelist)
      .putBoolean("autoDataSaver", clean.autoDataSaver)
      .putBoolean("autoPowerSave", clean.autoPowerSave)
      .putBoolean("killBackground", clean.killBackground)
      .putBoolean("disableLogs", clean.disableLogs)
      .putInt("phantomLimit", clean.phantomLimit)
      .putBoolean("wifiSleep", clean.wifiSleep)
      .putBoolean("scanAlways", clean.scanAlways)
      .putStringSet("heavyApps", clean.heavyApps)
      .putStringSet("lightApps", clean.lightApps)
      .putStringSet("bloat", clean.bloat)
      .putString("resolution", clean.resolution.key)
      .putInt("peakFps", clean.peakFps)
      .apply()
  }

  fun confFile(context: Context): File = File(EngineFiles.directory(context), "custom/features.conf")

  /** Must run before every apply and every feature command. */
  fun writeConf(
    context: Context,
    features: TuningSettings = load(context),
  ): File? {
    val file = confFile(context)
    return runCatching {
      file.parentFile?.mkdirs()
      file.writeText(features.conf())
      file.setReadable(true, false)
      file
    }.getOrNull()
  }
}
