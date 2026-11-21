package com.c0d3h01.androidtweaker.access

object RootConnection {
  @Volatile private var runner: RootProcess? = null

  @Volatile var lastError: String? = null
    private set

  /** Returns the open session without contacting a root manager. */
  fun runnerOrNull(): CommandRunner? = runner?.takeIf { it.isActive }

  fun ensure(): CommandRunner = runnerOrNull() ?: error(lastError ?: "Root access is not connected")

  /** Requests a new root session from an explicit user action or authorized boot reapply. */
  @Synchronized
  fun request(): CommandRunner {
    runnerOrNull()?.let { return it }
    runner?.close()
    val started = RootProcess()
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
