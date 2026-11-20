package com.c0d3h01.androidtweaker.profiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSettingsTest {
  @Test
  fun defaultsEnableEveryGroupAndOverrideNothing() {
    val conf = ProfileSettings().conf()
    ProfileGroup.entries.forEach { assertTrue(it.key, conf.contains(it.key)) }
    assertFalse(conf.contains("CPU_MAX_FREQ="))
    assertFalse(conf.contains("VM_SWAPPINESS="))
    assertFalse(conf.contains("IO_READ_AHEAD="))
  }

  @Test
  fun groupsAreWrittenInEngineOrderAndDisabledOnesDropped() {
    val prefs = ProfileSettings(groups = setOf(ProfileGroup.VM, ProfileGroup.CPU))
    assertTrue(prefs.conf().contains("TWEAK_GROUPS=\"cpu vm\""))
  }

  @Test
  fun noGroupsWritesAnEmptyListSoNothingApplies() {
    assertTrue(ProfileSettings(groups = emptySet()).conf().contains("TWEAK_GROUPS=\"\""))
  }

  @Test
  fun ceilingsArePercentOfDeviceMaximumAndGuarded() {
    val conf = ProfileSettings(cpuCeiling = 75, gpuCeiling = 60).conf()
    assertTrue(conf.contains("""[ -n "${'$'}{CPU_MAXFREQ:-}" ] && CPU_MAX_FREQ=${'$'}((CPU_MAXFREQ * 75 / 100))"""))
    assertTrue(conf.contains("""[ -n "${'$'}{GPU_MAXFREQ:-}" ] && GPU_MAX_FREQ_SET=${'$'}((GPU_MAXFREQ * 60 / 100))"""))
  }

  @Test
  fun limitsAreClampedBeforeTheyReachTheShell() {
    val conf = ProfileSettings(cpuCeiling = 5, swappiness = 900, readAheadKb = 1).conf()
    assertTrue(conf.contains("* 20 / 100"))
    assertTrue(conf.contains("VM_SWAPPINESS=200"))
    assertTrue(conf.contains("IO_READ_AHEAD=32"))
  }

  @Test
  fun presetRecommendedKeepsThermalThrottling() {
    val recommended = ProfileSettings().withPreset(ProfilePreset.RECOMMENDED)
    assertFalse(ProfileGroup.THERMAL in recommended.groups)
    assertEquals(ProfileGroup.entries.size - 1, recommended.enabledCount)
    assertTrue(
      recommended.conf().contains(
        "TWEAK_GROUPS=\"cpu gpu kernel sched stune io vm fs ksm doze network display renice system\"",
      ),
    )
  }

  @Test
  fun presetNoneAndAllAreTotal() {
    assertEquals(0, ProfileSettings().withPreset(ProfilePreset.NONE).enabledCount)
    assertEquals(
      ProfileGroup.entries.size,
      ProfileSettings(groups = emptySet()).withPreset(ProfilePreset.ALL).enabledCount,
    )
  }
}
