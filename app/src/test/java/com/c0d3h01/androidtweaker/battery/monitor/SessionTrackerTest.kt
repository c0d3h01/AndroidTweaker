package com.c0d3h01.androidtweaker.battery.monitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import com.c0d3h01.androidtweaker.battery.BatteryReading
import com.c0d3h01.androidtweaker.battery.BatteryStatus
import com.c0d3h01.androidtweaker.battery.Plugged
import org.junit.Test

class SessionTrackerTest {
  private fun reading(at: Long, level: Int? = 20, status: BatteryStatus = BatteryStatus.CHARGING,
    plugged: Plugged = Plugged.AC, counter: Long? = null, screenOn: Boolean = true) = BatteryReading(
    at, level, status, plugged, 1000, 4000, 30f, counter, 4000, null, null, null, screenOn,
  )

  @Test fun pluggedIdleDoesNotFinishChargeAndCounterWinsOverIntegration() {
    val tracker = SessionTracker()
    assertTrue(tracker.onReading(reading(0, counter = 1_000_000), 0, 0).isEmpty())
    assertTrue(tracker.onReading(reading(60_000, 35, BatteryStatus.IDLE, counter = 1_600_000), 60_000, 0).isEmpty())
    val session = tracker.onReading(reading(120_000, 35, BatteryStatus.DISCHARGING, Plugged.NONE, 1_600_000), 120_000, 0).single()
    assertTrue(session.charging)
    assertEquals(600, session.addedMah)
    assertEquals(4000, session.capacityMah)
    assertEquals(120_000L, session.screenOnMs)
  }

  @Test fun shortSessionIsDiscardedAndMissingLevelDoesNotChangeIt() {
    val tracker = SessionTracker()
    tracker.onReading(reading(0), 0, 0)
    assertTrue(tracker.onReading(reading(10_000, null), 10_000, 0).isEmpty())
    assertTrue(tracker.onReading(reading(40_000, plugged = Plugged.NONE, status = BatteryStatus.DISCHARGING), 40_000, 0).isEmpty())
  }

  @Test fun dischargeTracksPreviousScreenStateAndDeepSleep() {
    val tracker = SessionTracker()
    tracker.onReading(reading(0, 80, BatteryStatus.DISCHARGING, Plugged.NONE), 0, 0)
    tracker.onReading(reading(60_000, 79, BatteryStatus.DISCHARGING, Plugged.NONE, screenOn = false), 60_000, 0)
    val session = tracker.onReading(reading(120_000, 78, screenOn = false), 120_000, 30_000).single()
    assertFalse(session.charging)
    assertEquals(60_000L, session.screenOnMs)
    assertEquals(60_000L, session.screenOffMs)
    assertEquals(30_000L, session.deepSleepMs)
  }

  @Test fun currentFallbackCapsIntegrationAcrossLongSamplingGap() {
    val tracker = SessionTracker()
    tracker.onReading(reading(0), 0, 0)
    tracker.onReading(reading(600_000, 40), 600_000, 0)
    val session = tracker.onReading(reading(660_000, 40, BatteryStatus.DISCHARGING, Plugged.NONE), 660_000, 0).single()
    assertEquals(33, session.addedMah)
  }
}
