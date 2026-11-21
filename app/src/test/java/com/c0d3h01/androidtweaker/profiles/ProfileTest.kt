package com.c0d3h01.androidtweaker.profiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileTest {
  @Test
  fun mapsAll() {
    assertEquals(Profile.BATTERY, Profile.fromCode(1))
    assertEquals(Profile.BALANCED, Profile.fromCode(2))
    assertNull(Profile.fromCode(3))
    assertEquals(Profile.PERFORMANCE, Profile.fromCode(4))
    assertEquals(3, Profile.entries.size)
    assertEquals("gaming", Profile.PERFORMANCE.preferenceKey)
    assertEquals("performance", Profile.PERFORMANCE.commandKey)
  }

  @Test
  fun unknownIsNull() {
    assertNull(Profile.fromCode(0))
    assertNull(Profile.fromCode(5))
    assertNull(Profile.fromCode(9))
    assertNull(Profile.fromCode(null))
  }
}
