package com.c0d3h01.androidtweaker.util

import com.c0d3h01.androidtweaker.data.ShellRunner

object RootShell {
  @Volatile private var runner: SuProcessRunner? = null

  @Volatile var lastError: String? = null
    private set

  /** Returns the open session without contacting a root manager. */
  fun runnerOrNull(): ShellRunner? = runner?.takeIf { it.isActive }

  fun ensure(): ShellRunner = runnerOrNull() ?: error(lastError ?: "Root access is not connected")

  /** Requests a new root session from an explicit user action or authorized boot reapply. */
  @Synchronized
  fun request(): ShellRunner {
    runnerOrNull()?.let { return it }
    runner?.close()
    val started = SuProcessRunner()
    return try {
      check(started.isRoot()) { "Root access denied" }
      runner = started
      lastError = null
      started
    } catch (e: Exception) {
      started.close()
      lastError = e.message
      throw e
    }
  }
}
