package com.c0d3h01.androidtweaker.util

import com.c0d3h01.androidtweaker.data.TweakEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellAccessTest {
  @Test
  fun profileAccessFollowsTheShizukuServerUid() {
    val root = ShizukuAccess.modeForUid(0)
    val adb = ShizukuAccess.modeForUid(2000)

    assertEquals(ShellAccess.Mode.SHIZUKU_ROOT, root)
    assertTrue(root.canApplyProfiles)
    assertEquals(ShellAccess.Mode.SHIZUKU, adb)
    assertFalse(adb.canApplyProfiles)
    assertEquals(ShellAccess.Mode.NONE, ShizukuAccess.modeForUid(10000))
  }

  @Test
  fun discoveringRootDoesNotStartASession() {
    assertNull(RootShell.runnerOrNull())
  }

  @Test
  fun kernelCommandsRequireRoot() {
    assertTrue(TweakEngine.commandRequiresRoot("apply gaming"))
    assertTrue(TweakEngine.commandRequiresRoot("throttle off"))
    assertTrue(TweakEngine.commandRequiresRoot("charge"))
    assertTrue(TweakEngine.commandRequiresRoot("clean ram"))
    assertTrue(TweakEngine.commandRequiresRoot("clean cache"))
    assertFalse(TweakEngine.commandRequiresRoot("clean logs"))
    assertFalse(TweakEngine.commandRequiresRoot("doze force"))
  }
}
