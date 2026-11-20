package com.c0d3h01.androidtweaker.app

import com.c0d3h01.androidtweaker.access.AccessManager
import com.c0d3h01.androidtweaker.access.RootInfo
import com.c0d3h01.androidtweaker.engine.TweakEngine
import com.c0d3h01.androidtweaker.tuning.TuningSettings

data class AppState(
  val profileCode: Int,
  val profileAccess: AccessManager.Mode?,
  val features: TuningSettings,
  val reapplyOnBoot: Boolean,
  val activityNotifications: Boolean,
  val notificationsAllowed: Boolean,
  val access: AccessManager.Mode = AccessManager.Mode.NONE,
  val root: RootInfo? = null,
  val connecting: Boolean = false,
  val applying: Boolean = false,
  val pendingCode: Int? = null,
  val lastApply: TweakEngine.Result? = null,
  val showApplyFailure: Boolean = false,
  val message: String? = null,
)
