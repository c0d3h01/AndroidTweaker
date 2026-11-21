package com.c0d3h01.androidtweaker.profiles

import com.c0d3h01.androidtweaker.access.AccessManager
import com.c0d3h01.androidtweaker.engine.TweakEngine
import com.c0d3h01.androidtweaker.tuning.TuningSettings

/** Confirmed selection and the currently running tuning mutation. */
data class ProfileState(
  val profileCode: Int,
  val profileAccess: AccessManager.Mode?,
  val features: TuningSettings,
  val applying: Boolean = false,
  val pendingCode: Int? = null,
  val lastApply: TweakEngine.Result? = null,
  val showApplyFailure: Boolean = false,
) {
  fun beginApply(code: Int): ProfileState? =
    if (applying) null else copy(applying = true, pendingCode = code)

  fun completeApply(code: Int, mode: AccessManager.Mode, result: TweakEngine.Result): ProfileState = copy(
    profileCode = if (result.ok) code else profileCode,
    profileAccess = if (result.ok) mode else profileAccess,
    lastApply = result,
    showApplyFailure = !result.ok,
  )
}
