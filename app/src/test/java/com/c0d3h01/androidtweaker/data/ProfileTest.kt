package com.c0d3h01.androidtweaker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileTest {
  @Test
  fun mapsAll() {
    assertEquals(Profile.BATTERY, Profile.fromCode(1))
    assertEquals(Profile.BALANCED, Profile.fromCode(2))
    assertEquals(Profile.PERFORMANCE, Profile.fromCode(3))
    assertEquals(Profile.GAMING, Profile.fromCode(4))
  }

  @Test
  fun unknownIsNull() {
    assertNull(Profile.fromCode(0))
    assertNull(Profile.fromCode(5))
    assertNull(Profile.fromCode(9))
    assertNull(Profile.fromCode(null))
  }
}
