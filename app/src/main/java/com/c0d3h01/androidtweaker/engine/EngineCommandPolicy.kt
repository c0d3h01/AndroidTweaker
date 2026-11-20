package com.c0d3h01.androidtweaker.engine

/** Central root requirement for UI, boot and dispatcher callers. */
object EngineCommandPolicy {
  fun commandRequiresRoot(args: String): Boolean =
    when (args.substringBefore(' ')) {
      "apply", "charge", "misc", "throttle" -> true
      "clean" -> args == "clean ram" || args == "clean cache"
      else -> false
    }
}
