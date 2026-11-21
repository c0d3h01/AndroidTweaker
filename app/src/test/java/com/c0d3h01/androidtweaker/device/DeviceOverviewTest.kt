package com.c0d3h01.androidtweaker.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceOverviewTest {
  @Test
  fun `battery percentage supports nonstandard scales and preserves empty and full readings`() {
    assertEquals(67, batteryPercentage(2, 3))
    assertEquals(0, batteryPercentage(0, 100))
    assertEquals(100, batteryPercentage(Int.MAX_VALUE, Int.MAX_VALUE))
  }

  @Test
  fun `battery percentage rejects missing or inconsistent readings`() {
    assertNull(batteryPercentage(-1, 100))
    assertNull(batteryPercentage(0, 0))
    assertNull(batteryPercentage(101, 100))
  }

  @Test
  fun `capacity keeps zero available and rejects inconsistent measurements`() {
    assertEquals(DeviceCapacity(0, 100), deviceCapacity(0, 100))
    assertNull(deviceCapacity(-1, 100))
    assertNull(deviceCapacity(101, 100))
    assertNull(deviceCapacity(0, 0))
  }
}
