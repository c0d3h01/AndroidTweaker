package com.c0d3h01.androidtweaker.access

import com.c0d3h01.androidtweaker.engine.EngineCommandPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessManagerTest {
  @Test
  fun profileAccessFollowsTheShizukuServerUid() {
    val root = ShizukuConnection.modeForUid(0)
    val adb = ShizukuConnection.modeForUid(2000)

    assertEquals(AccessManager.Mode.SHIZUKU_ROOT, root)
    assertTrue(root.canApplyProfiles)
    assertEquals(AccessManager.Mode.SHIZUKU, adb)
    assertFalse(adb.canApplyProfiles)
    assertEquals(AccessManager.Mode.NONE, ShizukuConnection.modeForUid(10000))
  }

  @Test
  fun discoveringRootDoesNotStartASession() {
    assertNull(RootConnection.runnerOrNull())
  }

  @Test
  fun kernelCommandsRequireRoot() {
    assertTrue(EngineCommandPolicy.commandRequiresRoot("apply performance"))
    assertTrue(EngineCommandPolicy.commandRequiresRoot("throttle off"))
    assertTrue(EngineCommandPolicy.commandRequiresRoot("charge"))
    assertTrue(EngineCommandPolicy.commandRequiresRoot("clean ram"))
    assertTrue(EngineCommandPolicy.commandRequiresRoot("clean cache"))
    assertFalse(EngineCommandPolicy.commandRequiresRoot("clean logs"))
    assertFalse(EngineCommandPolicy.commandRequiresRoot("doze force"))
  }
}
