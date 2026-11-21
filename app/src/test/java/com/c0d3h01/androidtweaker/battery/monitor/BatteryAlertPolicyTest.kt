package com.c0d3h01.androidtweaker.battery.monitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.c0d3h01.androidtweaker.battery.BatteryReading
import com.c0d3h01.androidtweaker.battery.BatterySettings
import com.c0d3h01.androidtweaker.battery.BatteryStatus
import com.c0d3h01.androidtweaker.battery.Plugged
import org.junit.Test

class BatteryAlertPolicyTest {
  private fun reading(level: Int?, plugged: Boolean = false, temp: Float? = null) = BatteryReading(
    0, level, BatteryStatus.DISCHARGING, if (plugged) Plugged.AC else Plugged.NONE,
    null, null, temp, null, null, null, null, null, false,
  )

  @Test fun lowAlarmLatchesAndRisingLevelRearmsIt() {
    val policy = BatteryAlertPolicy()
    val settings = BatterySettings(notifyHighTemp = false)
    assertEquals(listOf("low"), policy.evaluate(reading(15), null, settings, 1).map { it.key })
    assertTrue(policy.evaluate(reading(14), reading(15), settings, 2).isEmpty())
    policy.onLevelChange(reading(16), reading(14))
    assertEquals(listOf("low"), policy.evaluate(reading(15), reading(16), settings, 3).map { it.key })
  }

  @Test fun targetRequiresCrossingInTheConnectionDirection() {
    val settings = BatterySettings(notifyLevel = true, notifyLow = false, notifyHighTemp = false)
    assertEquals(listOf("level"), BatteryAlertPolicy().evaluate(reading(80, true), reading(79, true), settings, 1).map { it.key })
    assertEquals(listOf("level"), BatteryAlertPolicy().evaluate(reading(80), reading(81), settings, 1).map { it.key })
    assertTrue(BatteryAlertPolicy().evaluate(reading(80), null, settings, 1).isEmpty())
  }

  @Test fun temperatureHasThirtyMinuteCooldownAndUnknownLevelSkipsAlarms() {
    val policy = BatteryAlertPolicy()
    val settings = BatterySettings(notifyLow = false)
    val start = 2_000_000L
    assertEquals(listOf("highTemp"), policy.evaluate(reading(50, temp = 45f), null, settings, start).map { it.key })
    assertTrue(policy.evaluate(reading(50, temp = 45f), null, settings, start + 1).isEmpty())
    assertEquals(1, policy.evaluate(reading(50, temp = 45f), null, settings, start + 30 * 60_000).size)
    assertTrue(policy.evaluate(reading(null, temp = 80f), null, settings, start + 60 * 60_000).isEmpty())
  }
}
