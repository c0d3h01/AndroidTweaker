package com.c0d3h01.androidtweaker.profiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import com.c0d3h01.androidtweaker.access.AccessManager
import com.c0d3h01.androidtweaker.engine.TweakEngine
import com.c0d3h01.androidtweaker.tuning.TuningSettings
import org.junit.Test

class ProfileStateTest {
  private val confirmed = ProfileState(2, AccessManager.Mode.ROOT, TuningSettings())

  @Test fun pendingApplyDoesNotChangeConfirmedSelectionAndRejectsAnotherTap() {
    val pending = confirmed.beginApply(4)!!
    assertEquals(2, pending.profileCode)
    assertEquals(4, pending.pendingCode)
    assertTrue(pending.applying)
    assertNull(pending.beginApply(1))
  }

  @Test fun failureKeepsConfirmedProfileAndRecordsFailure() {
    val failure = TweakEngine.Result(false, 126, "Root access required", "test.log", 0)
    val state = confirmed.beginApply(4)!!.completeApply(4, AccessManager.Mode.NONE, failure)
    assertEquals(2, state.profileCode)
    assertEquals(AccessManager.Mode.ROOT, state.profileAccess)
    assertEquals(failure, state.lastApply)
    assertTrue(state.showApplyFailure)
  }

  @Test fun successfulApplyConfirmsProfileAndBackend() {
    val success = TweakEngine.Result(true, 0, "Applied", "test.log", 0)
    val state = confirmed.beginApply(4)!!.completeApply(4, AccessManager.Mode.SHIZUKU_ROOT, success)
    assertEquals(4, state.profileCode)
    assertEquals(AccessManager.Mode.SHIZUKU_ROOT, state.profileAccess)
    assertFalse(state.showApplyFailure)
  }
}
