package com.c0d3h01.androidtweaker.util

import com.c0d3h01.androidtweaker.data.ShellRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class SuRunnerTest {
  @Test
  fun framesOutputWithTrickyLines() {
    SuProcessRunner(listOf("sh")).use { runner ->
      assertEquals("a\n__AT_X__\nb", runner.run("printf 'a\\n__AT_X__\\nb'").trimEnd())
      assertEquals("multiline\nok", runner.run("echo 'multiline'\necho ok").trimEnd())
    }
  }

  @Test
  fun timesOutAndLeavesTheSessionClosed() {
    SuProcessRunner(listOf("sh"), timeoutMillis = 100).use { runner ->
      assertThrows(IllegalStateException::class.java) { runner.run("sleep 2") }
      assertFalse(runner.isActive)
      assertThrows(IllegalStateException::class.java) { runner.run("echo unexpected") }
    }
  }

  @Test
  fun interruptedCommandIsNeverReplayed() {
    val journal = Files.createTempFile("shell-replay", ".log").toFile()
    try {
      val shell = "read -r _; echo ran >> '${journal.absolutePath}'; exit 1"
      SuProcessRunner(listOf("sh", "-c", shell)).use { runner ->
        assertThrows(IllegalStateException::class.java) { runner.run("echo command") }
        assertFalse(runner.isActive)
        assertEquals(listOf("ran"), journal.readLines())
      }
    } finally {
      assertTrue(journal.delete())
    }
  }

  @Test
  fun rootCheckRequiresTheActualUid() {
    assertTrue(ShellRunner { "0\n" }.isRoot())
    assertFalse(ShellRunner { "uid=0 is required\n2000\n" }.isRoot())
  }
}
