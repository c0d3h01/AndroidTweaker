package com.c0d3h01.androidtweaker.battery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryJsonTest {
  @Test fun legacySettingsKeepDefaultsAndIgnoreUnknownKeys() {
    val settings = BatteryJson.settings("""{"monitorEnabled":true,"levelAlarm":90,"future":123}""")
    assertTrue(settings.monitorEnabled)
    assertEquals(90, settings.levelAlarm)
    assertEquals(15, settings.lowLevel)
    assertTrue(settings.notifyHighTemp)
    val encoded = BatteryJson.run { settings.toJson().toString() }
    assertEquals(settings, BatteryJson.settings(encoded))
  }

  @Test fun invalidSettingsFallBackWithoutCrashing() {
    assertEquals(BatterySettings(), BatteryJson.settings(null))
    assertEquals(BatterySettings(), BatteryJson.settings("not-json"))
    val settings = BatteryJson.settings("""{"refreshSeconds":1,"lowLevel":90,"highTempC":99}""")
    assertEquals(2, settings.refreshSeconds)
    assertEquals(50, settings.lowLevel)
    assertEquals(70, settings.highTempC)
  }

  @Test fun compactSamplesPreserveUnknownSensorsAndRoundTrip() {
    val raw = """[{"t":1000,"l":42,"g":false,"s":true,"future":123},{"t":2000,"l":43,"c":30.5,"i":-100,"v":4000,"g":true,"s":false}]"""
    val samples = BatteryJson.samples(raw)
    assertEquals(2, samples.size)
    assertNull(samples.first().tempC)
    assertNull(samples.first().currentMa)
    assertNull(samples.first().voltageMv)
    assertEquals(-100, samples.last().currentMa)
    assertEquals(samples, BatteryJson.samples(BatteryJson.samplesJson(samples).toString()))
  }

  @Test fun legacySessionsPreserveFieldsAndUnknownChargerFallsBack() {
    val raw = """[{"a":1000,"b":61000,"c":true,"d":20,"e":40,"f":800,"g":10.5,"h":35.5,"i":"AC","j":95,"k":4000,"l":10000,"m":50000,"n":30000},{"a":70000,"b":130000,"g":0,"i":"FUTURE"}]"""
    val sessions = BatteryJson.sessions(raw)
    assertEquals(2, sessions.size)
    assertEquals(800, sessions.first().addedMah)
    assertEquals(4000, sessions.first().capacityMah)
    assertEquals(30000L, sessions.first().deepSleepMs)
    assertEquals(Plugged.NONE, sessions.last().charger)
    assertNull(sessions.last().capacityMah)
    assertNull(sessions.last().peakTempC)
    assertEquals(sessions, BatteryJson.sessions(BatteryJson.sessionsJson(sessions).toString()))
  }
}
