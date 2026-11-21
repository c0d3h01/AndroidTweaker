package com.c0d3h01.androidtweaker.engine

import com.c0d3h01.androidtweaker.profiles.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineCommandsTest {
  @Test
  fun applyUsesProfileNameAndDirectDispatcher() {
    val cmd = EngineCommands.apply("/data/user/0/app/files/tweaker", Profile.PERFORMANCE, "2026-11-15 10:00:00")
    assertTrue(cmd.contains("MODDIR='/data/user/0/app/files/tweaker'"))
    assertTrue(cmd.contains("sh '/data/user/0/app/files/tweaker/dispatcher.sh' 'apply' 'performance'"))
    assertTrue(cmd.contains("2026-11-15 10:00:00"))
  }

  @Test
  fun miscRoutesMaintenance() {
    val cmd = EngineCommands.misc("/tmp/engine", "stamp")
    assertTrue(cmd.contains("dispatcher.sh' 'misc'"))
  }

  @Test
  fun forwardsArgumentsAsLiteralShellWords() {
    val cmd = EngineCommands.run("/tmp/engine", "restore app's;name", "stamp")
    assertTrue(cmd.contains("dispatcher.sh' 'restore' 'app'\\''s;name' 2>&1"))
  }

  @Test
  fun parseReadsExitCodeAndStripsMarker() {
    val parsed = EngineCommands.parse("write: cpu0 100 -> 200\n__AT_EXIT:0")
    assertEquals(0, parsed.exitCode)
    assertEquals("write: cpu0 100 -> 200", parsed.output)
  }

  @Test
  fun parseFailure() {
    val parsed = EngineCommands.parse("denied\n__AT_EXIT:1")
    assertEquals(1, parsed.exitCode)
    assertFalse(parsed.exitCode == 0)
  }

  @Test
  fun missingMarkerIsFailure() {
    assertEquals(-1, EngineCommands.parse("shell died").exitCode)
  }

  @Test
  fun quoteEscapesSingleQuotes() {
    assertEquals("'a'\\''b'", EngineCommands.quote("a'b"))
  }
}
