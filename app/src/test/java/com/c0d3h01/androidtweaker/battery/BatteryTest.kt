package com.c0d3h01.androidtweaker.battery

import com.c0d3h01.androidtweaker.battery.analysis.chargeQuality
import com.c0d3h01.androidtweaker.battery.analysis.estimateHealth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryTest {
  @Test
  fun currentUsesMicroampsAtEveryMagnitude() {
    assertEquals(-4, batteryCurrentMa(-4_000))
    assertEquals(1_250, batteryCurrentMa(1_250_000))
    assertEquals(0, batteryCurrentMa(0))
    assertNull(batteryCurrentMa(Int.MIN_VALUE))
    assertNull(batteryCurrentMa(null))
  }

  @Test
  fun gentleChargeScoresHigh() {
    val score = chargeQuality(startLevel = 30, endLevel = 60, peakTempC = 30f, peakPowerW = 10f)
    assertTrue("gentle charge should be >= 90, was $score", score >= 90)
  }

  @Test
  fun hotFullChargeScoresLow() {
    val score = chargeQuality(startLevel = 5, endLevel = 100, peakTempC = 48f, peakPowerW = 40f)
    assertTrue("stressed charge should be < 60, was $score", score < 60)
  }

  @Test
  fun unknownSensorsStillScore() {
    val score = chargeQuality(startLevel = 20, endLevel = 50, peakTempC = null, peakPowerW = null)
    assertTrue("score out of range: $score", score in 0..100)
  }

  @Test
  fun healthUsesPhoneCapacityWhenPlausible() {
    val health = estimateHealth(emptyList(), designMah = 4000, phoneFullMah = 3600, cycles = 120)
    assertEquals(3600, health.capacityMah)
    assertEquals(90, health.percent)
    assertEquals(120, health.cycles)
  }

  @Test
  fun healthIgnoresImplausiblePhoneCapacity() {
    val health = estimateHealth(emptyList(), designMah = 4000, phoneFullMah = 40, cycles = null)
    assertNull(health.capacityMah)
    assertEquals("Learning", health.confidence)
  }

  @Test
  fun healthNeedsDesignCapacityToEstimate() {
    val session =
      BatterySession(
        start = 0,
        end = 3_600_000,
        charging = true,
        startLevel = 20,
        endLevel = 80,
        addedMah = 2400,
        peakPowerW = 10f,
        peakTempC = 30f,
        charger = Plugged.AC,
        quality = 90,
        capacityMah = 4000,
        screenOnMs = 0,
        screenOffMs = 3_600_000,
        deepSleepMs = 0,
      )
    val withoutDesign = estimateHealth(listOf(session), null, null, null)
    assertNull(withoutDesign.percent)
    val withDesign = estimateHealth(listOf(session), 5000, null, null)
    assertEquals(80, withDesign.percent)
    assertNotNull(withDesign.capacityMah)
  }

  @Test
  fun unitsStayWithTheirNumbers() {
    assertEquals("37.0\u00A0°C", formatTemp(37f))
    assertEquals("1.5\u00A0W", formatPower(1.5f))
    assertEquals("4200\u00A0mAh", formatMah(4200))
    assertEquals("+1200\u00A0mA", formatCurrent(1200))
    assertEquals("4.20\u00A0V", formatVoltage(4200))
    assertEquals("5h\u00A000m", formatDuration(5 * 3_600_000L))
  }
}
