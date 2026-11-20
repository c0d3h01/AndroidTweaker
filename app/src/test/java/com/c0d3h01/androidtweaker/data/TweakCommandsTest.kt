package com.c0d3h01.androidtweaker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TweakCommandsTest {
  @Test
  fun applyUsesProfileNameAndDirectDispatcher() {
    val cmd = TweakCommands.apply("/data/user/0/app/files/tweaker", Profile.GAMING, "2026-11-15 10:00:00")
    assertTrue(cmd.contains("MODDIR='/data/user/0/app/files/tweaker'"))
    assertTrue(cmd.contains("sh '/data/user/0/app/files/tweaker/lib/dispatcher.sh' 'apply' 'gaming'"))
    assertTrue(cmd.contains("2026-11-15 10:00:00"))
  }

  @Test
  fun miscRoutesMaintenance() {
    val cmd = TweakCommands.misc("/tmp/engine", "stamp")
    assertTrue(cmd.contains("dispatcher.sh' 'misc'"))
  }

  @Test
  fun forwardsArgumentsAsLiteralShellWords() {
    val cmd = TweakCommands.run("/tmp/engine", "restore app's;name", "stamp")
    assertTrue(cmd.contains("dispatcher.sh' 'restore' 'app'\\''s;name' 2>&1"))
  }

  @Test
  fun parseReadsExitCodeAndStripsMarker() {
    val parsed = TweakCommands.parse("write: cpu0 100 -> 200\n__AT_EXIT:0")
    assertEquals(0, parsed.exitCode)
    assertEquals("write: cpu0 100 -> 200", parsed.output)
  }

  @Test
  fun parseFailure() {
    val parsed = TweakCommands.parse("denied\n__AT_EXIT:1")
    assertEquals(1, parsed.exitCode)
    assertFalse(parsed.exitCode == 0)
  }

  @Test
  fun missingMarkerIsFailure() {
    assertEquals(-1, TweakCommands.parse("shell died").exitCode)
  }

  @Test
  fun quoteEscapesSingleQuotes() {
    assertEquals("'a'\\''b'", TweakCommands.quote("a'b"))
  }
}
