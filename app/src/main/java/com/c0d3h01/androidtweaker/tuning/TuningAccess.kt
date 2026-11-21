package com.c0d3h01.androidtweaker.tuning

import com.c0d3h01.androidtweaker.access.AccessManager.Mode

/** Requirements are shared by the UI and settings mutation boundary. */
enum class TuningFeature(val needsRoot: Boolean = false) {
  PROFILES(true), CHARGING(true), THERMAL(true), LOGGING(true), ROOT_MAINTENANCE(true),
  POWER, NETWORK, DISPLAY, PACKAGES, MAINTENANCE;

  fun available(mode: Mode): Boolean = if (needsRoot) mode.canApplyProfiles else mode != Mode.NONE
}

object TuningAccess {
  fun allowedChanges(current: TuningSettings, requested: TuningSettings, mode: Mode): TuningSettings {
    if (mode == Mode.NONE) return current
    val clean = requested.sanitized()
    return if (mode.canApplyProfiles) clean else clean.copy(
      charging = current.charging,
      throttleOff = current.throttleOff,
      disableLogs = current.disableLogs,
    )
  }
}
