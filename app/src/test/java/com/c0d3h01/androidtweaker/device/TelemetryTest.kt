package com.c0d3h01.androidtweaker.device

import com.c0d3h01.androidtweaker.common.normalizeTempC
import com.c0d3h01.androidtweaker.profiles.profileLevel
import com.c0d3h01.androidtweaker.settings.LogLevel
import com.c0d3h01.androidtweaker.settings.classifyLogLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryTest {
  private fun core(
    index: Int,
    min: Long,
    max: Long,
    cur: Long = max / 2,
    online: Boolean = true,
  ) = CoreSample(
    index = index,
    online = online,
    curKHz = cur,
    minKHz = min,
    maxKHz = max,
    governor = "schedutil",
  )

  @Test
  fun coresSplitIntoConsecutiveClusters() {
    val cores =
      listOf(
        core(0, 300_000, 1_800_000),
        core(1, 300_000, 1_800_000),
        core(2, 300_000, 1_800_000),
        core(3, 300_000, 1_800_000),
        core(4, 800_000, 2_400_000),
        core(5, 800_000, 2_400_000),
        core(6, 800_000, 2_400_000),
        core(7, 1_400_000, 2_800_000, cur = 2_800_000),
      )
    val clusters = clusterCores(cores)
    assertEquals(3, clusters.size)
    assertEquals(listOf("LITTLE", "MID", "PRIME"), clusters.map { it.label })
    assertEquals(4, clusters[0].cores.size)
    assertEquals(3, clusters[1].cores.size)
    assertEquals(1, clusters[2].cores.size)
    assertEquals(2_800_000L, clusters[2].curKHz)
    assertEquals(2_800_000L, clusters[2].maxKHz)
  }

  @Test
  fun singleClusterIsLabelledCluster() {
    val clusters = clusterCores(listOf(core(0, 300_000, 1_800_000), core(1, 300_000, 1_800_000)))
    assertEquals(1, clusters.size)
    assertEquals("CLUSTER", clusters[0].label)
  }

  @Test
  fun thermalUnitsNormalise() {
    assertEquals(45f, normalizeTempC(45_000)!!, 0.01f)
    assertEquals(45f, normalizeTempC(450)!!, 0.01f)
    assertEquals(45f, normalizeTempC(45)!!, 0.01f)
    assertNull(normalizeTempC(0))
    assertNull(normalizeTempC(500_000))
  }

  @Test
  fun meminfoParsesKilobytesToBytes() {
    val pair =
      parseMeminfo(
        """
        MemTotal:       8000000 kB
        MemFree:         200000 kB
        MemAvailable:   3000000 kB
        """.trimIndent(),
      )
    assertEquals(8_000_000L * 1024, pair?.first)
    assertEquals(3_000_000L * 1024, pair?.second)
  }

  @Test
  fun thermalNamesAreReadable() {
    assertEquals("CPU 0", friendlyThermalName("cpu-0-0-usr"))
    assertEquals("SoC", friendlyThermalName("soc-thermal"))
    assertEquals("GPU", friendlyThermalName("gpu-thermal"))
    assertEquals("Skin", friendlyThermalName("skin-therm"))
    assertEquals("Battery", friendlyThermalName("battery"))
  }

  @Test
  fun logClassificationUsesSeverityWords() {
    assertEquals(LogLevel.ERROR, classifyLogLine("EXT4-fs: failed to mount"))
    assertEquals(LogLevel.WARN, classifyLogLine("CPU7 throttle event tripped at 78C"))
    assertEquals(LogLevel.OK, classifyLogLine("su request granted to pid 4821"))
    assertEquals(LogLevel.INFO, classifyLogLine("Cluster0 entered C3-deep sleep"))
  }

  @Test
  fun profileLevelMapsCodes() {
    assertEquals(1, profileLevel(1))
    assertEquals(3, profileLevel(4))
    assertEquals(0, profileLevel(3))
    assertEquals(0, profileLevel(null))
    assertEquals(0, profileLevel(9))
  }

  @Test
  fun frequencyFormattingIsHumanReadable() {
    assertTrue(formatKHz(2_800_000).contains("GHz"))
    assertTrue(formatKHz(1_800_000).contains("GHz"))
    assertTrue(formatHz(587_000_000).contains("MHz"))
    assertEquals("—", formatKHz(null))
  }

  @Test
  fun unitsStayWithTheirNumbers() {
    assertEquals("2.80\u00A0GHz", formatKHz(2_800_000))
    assertEquals("587\u00A0MHz", formatHz(587_000_000))
    assertEquals("1.5\u00A0GB", formatBytes(1_610_612_736))
  }
}
