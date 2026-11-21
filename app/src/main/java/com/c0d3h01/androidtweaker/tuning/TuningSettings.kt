package com.c0d3h01.androidtweaker.tuning

import com.c0d3h01.androidtweaker.charging.ChargingSettings

/** Resolution presets the engine understands through `wm size`. */
enum class Resolution(
  val key: String,
  val label: String,
) {
  DEFAULT("default", "Native"),
  LOW("low", "75%"),
  WIDE("16:9", "16:9"),
}

/**
 * Global feature switches that are not tied to a profile. They are persisted
 * here, mirrored into `custom/features.conf` and enforced either by the engine
 * (boot/apply) or by [ChargingController] on every battery monitor tick.
 *
 * Zero/empty means "leave the device alone", so a fresh install changes
 * nothing until the user opts in.
 */
data class TuningSettings(
  val charging: ChargingSettings = ChargingSettings(),
  val throttleOff: Boolean = false,
  val forceDoze: Boolean = false,
  val dozeWhitelist: Set<String> = emptySet(),
  val autoDataSaver: Boolean = false,
  val autoPowerSave: Boolean = false,
  val killBackground: Boolean = false,
  val disableLogs: Boolean = false,
  val phantomLimit: Int = 0,
  val wifiSleep: Boolean = false,
  val scanAlways: Boolean = false,
  val heavyApps: Set<String> = emptySet(),
  val lightApps: Set<String> = emptySet(),
  val bloat: Set<String> = emptySet(),
  val resolution: Resolution = Resolution.DEFAULT,
  val peakFps: Int = 0,
) {
  /** Clamp anything a stored preference or a migration could have pushed out of range. */
  fun sanitized(): TuningSettings {
    val validHeavy = heavyApps.mapNotNull { cleanPkg(it) }.toSet()
    val validLight = lightApps.mapNotNull { cleanPkg(it) }.toSet()
    return copy(
      charging = charging.sanitized(),
      phantomLimit = if (phantomLimit == 0) 0 else phantomLimit.coerceIn(PHANTOM_RANGE),
      peakFps = if (peakFps == 0) 0 else peakFps.coerceIn(FPS_RANGE),
      // A package cannot be heavy and light at once; light wins the tie.
      heavyApps = validHeavy - validLight,
      lightApps = validLight,
      dozeWhitelist = dozeWhitelist.mapNotNull { cleanPkg(it) }.toSet(),
      bloat = bloat.mapNotNull { cleanPkg(it) }.toSet(),
    )
  }

  /** Shell fragment sourced by the engine before any feature command. */
  fun conf(): String {
    val clean = sanitized()

    fun flag(value: Boolean): String = if (value) "1" else "0"

    fun list(values: Set<String>): String = values.sorted().joinToString(" ")
    return buildString {
      appendLine("# AndroidTweaker — generated feature switches. Rewritten on every change.")
      appendLine("FEATURE_CHARGE_LIMIT=${if (clean.charging.chargeLimit == 0) "" else clean.charging.chargeLimit}")
      appendLine("FEATURE_CHARGE_BYPASS=${flag(clean.charging.chargeBypass)}")
      appendLine("FEATURE_TEMP_FUSE=${if (clean.charging.tempFuse == 0) "" else clean.charging.tempFuse}")
      appendLine("FEATURE_THROTTLE_OFF=${flag(clean.throttleOff)}")
      appendLine("FEATURE_FORCE_DOZE=${flag(clean.forceDoze)}")
      appendLine("FEATURE_DOZE_WHITELIST=\"${list(clean.dozeWhitelist)}\"")
      appendLine("FEATURE_AUTO_DATA_SAVER=${flag(clean.autoDataSaver)}")
      appendLine("FEATURE_AUTO_POWER_SAVE=${flag(clean.autoPowerSave)}")
      appendLine("FEATURE_KILL_BG=${flag(clean.killBackground)}")
      appendLine("FEATURE_DISABLE_LOGS=${flag(clean.disableLogs)}")
      appendLine("FEATURE_PHANTOM_LIMIT=${if (clean.phantomLimit == 0) "" else clean.phantomLimit}")
      appendLine("FEATURE_WIFI_SLEEP=${flag(clean.wifiSleep)}")
      appendLine("FEATURE_SCAN_ALWAYS=${flag(clean.scanAlways)}")
      appendLine("FEATURE_HEAVY=\"${list(clean.heavyApps)}\"")
      appendLine("FEATURE_LIGHT=\"${list(clean.lightApps)}\"")
      appendLine("FEATURE_BLOAT=\"${list(clean.bloat)}\"")
      appendLine("RESOLUTION=${clean.resolution.key}")
      appendLine("PEAK_FPS=${clean.peakFps}")
    }
  }

  companion object {
    val PHANTOM_RANGE = 4..64
    val FPS_RANGE = 30..240

    private val PACKAGE = Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+){1,}")

    internal fun cleanPkg(value: String): String? = value.trim().takeIf { PACKAGE.matches(it) }
  }
}
