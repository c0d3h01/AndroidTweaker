package com.c0d3h01.androidtweaker.access

import rikka.shizuku.Shizuku

object AccessManager {
  enum class Mode(
    val label: String,
    val canApplyProfiles: Boolean = false,
  ) {
    ROOT("Root", true),
    SHIZUKU_ROOT("Shizuku (root)", true),
    SHIZUKU("Shizuku (ADB)"),
    NONE("No shell"),
  }

  fun mode(): Mode = if (RootConnection.runnerOrNull() != null) Mode.ROOT else ShizukuConnection.mode()

  fun modeOf(runner: CommandRunner): Mode =
    when {
      runner is RootProcess -> Mode.ROOT
      runner === ShizukuConnection.runnerFor(Mode.SHIZUKU_ROOT) -> Mode.SHIZUKU_ROOT
      runner === ShizukuConnection.runnerFor(Mode.SHIZUKU) -> Mode.SHIZUKU
      else -> Mode.NONE
    }

  fun runnerOrNull(requireRoot: Boolean = false): CommandRunner? {
    RootConnection.runnerOrNull()?.let { return it }
    val mode = ShizukuConnection.mode()
    return if (!requireRoot || mode.canApplyProfiles) ShizukuConnection.runnerFor(mode) else null
  }

  fun ensure(requireRoot: Boolean = false): CommandRunner =
    runnerOrNull(requireRoot) ?: error(if (requireRoot) "Connect root to continue" else "Connect root or Shizuku to continue")
}
