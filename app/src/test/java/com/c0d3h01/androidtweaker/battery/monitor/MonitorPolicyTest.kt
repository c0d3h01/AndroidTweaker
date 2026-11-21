package com.c0d3h01.androidtweaker.battery.monitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MonitorPolicyTest {
  @Test
  fun interactiveUpdatesRespectTheStoredIntervalWithinSafeBounds() {
    assertEquals(15_000L, delay(screenOn = true))
    assertEquals(40_000L, delay(refreshSeconds = 40, screenOn = true))
    assertEquals(60_000L, delay(refreshSeconds = 90, screenOn = true))
  }

  @Test
  fun chargingKeepsLiveReadingsWithTheScreenOff() {
    assertEquals(30_000L, delay(plugged = true, charging = true))
  }

  @Test
  fun chargeControlOnlyNeedsFrequentChecksWhilePluggedIn() {
    assertEquals(20_000L, delay(plugged = true, chargeControl = true))
    assertEquals(20_000L, delay(refreshSeconds = 60, screenOn = true, plugged = true, chargeControl = true))
    assertNull(delay(chargeControl = true))
  }

  @Test
  fun idleReportsAndThermalAlertsShareASlowFallback() {
    assertEquals(300_000L, delay(needsIdleSample = true))
    assertNull(delay())
    assertNull(delay(plugged = true))
  }

  private fun delay(
    refreshSeconds: Int = 5,
    screenOn: Boolean = false,
    plugged: Boolean = false,
    charging: Boolean = false,
    chargeControl: Boolean = false,
    needsIdleSample: Boolean = false,
  ): Long? = monitorRefreshDelayMs(refreshSeconds, screenOn, plugged, charging, chargeControl, needsIdleSample)
}
