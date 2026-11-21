package com.c0d3h01.androidtweaker.tuning

import com.c0d3h01.androidtweaker.access.AccessManager.Mode
import com.c0d3h01.androidtweaker.charging.ChargingSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TuningAccessTest {
  @Test fun noBackendDisablesEveryPrivilegedGroup() {
    TuningFeature.entries.forEach { assertFalse(it.available(Mode.NONE)) }
  }

  @Test fun adbShizukuAllowsShellGroupsButNotRootGroups() {
    assertTrue(TuningFeature.DISPLAY.available(Mode.SHIZUKU))
    assertTrue(TuningFeature.POWER.available(Mode.SHIZUKU))
    assertTrue(TuningFeature.PACKAGES.available(Mode.SHIZUKU))
    assertFalse(TuningFeature.CHARGING.available(Mode.SHIZUKU))
    assertFalse(TuningFeature.PROFILES.available(Mode.SHIZUKU))
    assertFalse(TuningFeature.THERMAL.available(Mode.SHIZUKU))
    assertFalse(TuningFeature.LOGGING.available(Mode.SHIZUKU))
  }

  @Test fun bothRootBackendsAllowAllGroups() {
    TuningFeature.entries.forEach {
      assertTrue(it.available(Mode.ROOT))
      assertTrue(it.available(Mode.SHIZUKU_ROOT))
    }
  }

  @Test fun aStaleCallbackCannotSaveUnavailableSettings() {
    val current = TuningSettings()
    val requested = current.copy(charging = ChargingSettings(chargeLimit = 80), throttleOff = true, disableLogs = true, forceDoze = true)
    assertEquals(current, TuningAccess.allowedChanges(current, requested, Mode.NONE))
    val adb = TuningAccess.allowedChanges(current, requested, Mode.SHIZUKU)
    assertEquals(current.charging, adb.charging)
    assertFalse(adb.throttleOff)
    assertFalse(adb.disableLogs)
    assertTrue(adb.forceDoze)
    assertEquals(requested, TuningAccess.allowedChanges(current, requested, Mode.ROOT))
  }
}
