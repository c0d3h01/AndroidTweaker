package com.c0d3h01.androidtweaker.tuning

import com.c0d3h01.androidtweaker.charging.ChargingSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TuningSettingsTest {
  @Test
  fun defaultsLeaveTheDeviceAlone() {
    val conf = TuningSettings().conf()
    assertTrue(conf.contains("FEATURE_CHARGE_LIMIT=\n"))
    assertTrue(conf.contains("FEATURE_CHARGE_BYPASS=0"))
    assertTrue(conf.contains("FEATURE_TEMP_FUSE=\n"))
    assertTrue(conf.contains("FEATURE_FORCE_DOZE=0"))
    assertTrue(conf.contains("FEATURE_THROTTLE_OFF=0"))
    assertTrue(conf.contains("FEATURE_PHANTOM_LIMIT=\n"))
    assertTrue(conf.contains("RESOLUTION=default"))
    assertTrue(conf.contains("PEAK_FPS=0"))
  }

  @Test
  fun valuesAreClampedAndPackagesValidated() {
    val clean =
      TuningSettings(
        charging = ChargingSettings(chargeLimit = 5, tempFuse = 90),
        phantomLimit = 1,
        peakFps = 10,
        heavyApps = setOf("com.game", "not a package", ""),
        lightApps = setOf("com.light"),
        bloat = setOf("com.oem.bloat"),
      ).sanitized()
    assertEquals(50, clean.charging.chargeLimit)
    assertEquals(60, clean.charging.tempFuse)
    assertEquals(4, clean.phantomLimit)
    assertEquals(30, clean.peakFps)
    assertEquals(setOf("com.game"), clean.heavyApps)
    assertEquals(setOf("com.oem.bloat"), clean.bloat)
  }

  @Test
  fun aPackageCannotBeHeavyAndLight() {
    val clean =
      TuningSettings(
        heavyApps = setOf("com.both", "com.heavy"),
        lightApps = setOf("com.both", "com.light"),
      ).sanitized()
    assertEquals(setOf("com.heavy"), clean.heavyApps)
    assertEquals(setOf("com.both", "com.light"), clean.lightApps)
  }

  @Test
  fun confWritesEngineFlagsAndSortedLists() {
    val conf =
      TuningSettings(
        charging = ChargingSettings(chargeLimit = 80, tempFuse = 45),
        forceDoze = true,
        dozeWhitelist = setOf("com.android.deskclock", "com.example.app"),
        heavyApps = setOf("com.zzz", "com.aaa"),
        resolution = Resolution.LOW,
        peakFps = 60,
      ).conf()
    assertTrue(conf.contains("FEATURE_CHARGE_LIMIT=80"))
    assertTrue(conf.contains("FEATURE_TEMP_FUSE=45"))
    assertTrue(conf.contains("FEATURE_FORCE_DOZE=1"))
    assertTrue(conf.contains("FEATURE_DOZE_WHITELIST=\"com.android.deskclock com.example.app\""))
    assertTrue(conf.contains("FEATURE_HEAVY=\"com.aaa com.zzz\""))
    assertTrue(conf.contains("RESOLUTION=low"))
    assertTrue(conf.contains("PEAK_FPS=60"))
  }

  @Test
  fun chargingActiveOnlyWhenSomethingIsConfigured() {
    assertFalse(ChargingSettings().active)
    assertTrue(ChargingSettings(chargeLimit = 80).active)
    assertTrue(ChargingSettings(chargeBypass = true).active)
    assertTrue(ChargingSettings(tempFuse = 45).active)
  }
}
