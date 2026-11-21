package com.c0d3h01.androidtweaker.battery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatterySensorsTest {
  @Test fun zeroFrameworkCurrentDoesNotHideReadableDischargeCurrent() {
    assertEquals(-650, resolveBatteryCurrentMa(0, listOf(0, -650_000), -500_000, allowZero = false))
  }

  @Test fun unavailableInstantaneousCurrentFallsBackToAverage() {
    assertEquals(-500, resolveBatteryCurrentMa(Int.MIN_VALUE, emptyList(), -500_000, allowZero = false))
  }

  @Test fun instantaneousReadingWinsAndUnitsAreNotGuessed() {
    assertEquals(-4, resolveBatteryCurrentMa(-4_000, listOf(-600_000), -500_000, allowZero = false))
  }

  @Test fun dischargingWithoutSensorsIsUnavailableRatherThanZeroWatts() {
    assertNull(resolveBatteryCurrentMa(0, listOf(0), 0, allowZero = false))
    assertNull(batteryPowerWatts(null, 4000))
    assertNull(batteryPowerWatts(-500, 0))
    assertEquals(2f, batteryPowerWatts(-500, 4000)!!, 0.001f)
  }

  @Test fun idlePluggedBatteryCanReportARealZero() {
    assertEquals(0, resolveBatteryCurrentMa(0, emptyList(), null, allowZero = true))
    assertNull(resolveBatteryCurrentMa(null, emptyList(), null, allowZero = true))
  }

  @Test fun readsTheNextSupplyWhenTheFirstIsMissingOrZero() {
    val paths = mutableListOf<String>()
    val sensors = BatterySensors { path ->
      paths.add(path)
      when (path) {
        "/sys/class/power_supply/battery/current_now" -> "0"
        "/sys/class/power_supply/bms/current_now" -> "-650000"
        "/sys/class/power_supply/bms/voltage_now" -> "4000000"
        else -> null
      }
    }
    assertEquals(listOf(0L, -650000L), sensors.longs("current_now"))
    assertEquals("4000000", sensors.text("voltage_now"))
  }

  @Test fun zeroVoltageDoesNotHideTheSecondSupply() {
    assertEquals(4000, resolveBatteryVoltageMv(0, listOf(0, 4_000_000)))
    assertEquals(3900, resolveBatteryVoltageMv(3900, listOf(4_000_000)))
    assertNull(resolveBatteryVoltageMv(0, listOf(0)))
  }

  @Test fun smallNonzeroPowerDoesNotRoundToZero() {
    assertEquals("0.04", formatPowerValue(0.04f))
    assertEquals("0.004", formatPowerValue(0.004f))
    assertEquals("1.5", formatPowerValue(1.5f))
  }
}
