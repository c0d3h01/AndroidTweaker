package com.c0d3h01.androidtweaker.settings

import com.c0d3h01.androidtweaker.access.CommandRunner
import com.c0d3h01.androidtweaker.engine.EngineCommands

enum class LogSource(
  val title: String,
) {
  TWEAKS("Tweaks"),
  DMESG("Kernel"),
  LOGCAT("System"),
}

fun LogSource.path(tweaksPath: String): String =
  when (this) {
    LogSource.TWEAKS -> tweaksPath
    LogSource.DMESG -> "dmesg"
    LogSource.LOGCAT -> "logcat"
  }

data class LogResult(
  val source: LogSource,
  val lines: List<String>,
  val missing: Boolean = false,
  val error: String? = null,
)

object LogReader {
  fun read(
    source: LogSource,
    runner: CommandRunner,
    tweaksPath: String,
    maxLines: Int = 400,
  ): LogResult {
    val command =
      when (source) {
        LogSource.TWEAKS -> {
          val path = EngineCommands.quote(tweaksPath)
          "if [ -f $path ]; then tail -n $maxLines $path; else echo @MISSING@; fi"
        }

        LogSource.DMESG -> {
          "dmesg 2>&1 | tail -n $maxLines"
        }

        LogSource.LOGCAT -> {
          "logcat -d -v threadtime -t $maxLines 2>&1"
        }
      }
    val raw =
      try {
        runner.run(command)
      } catch (e: Exception) {
        return LogResult(source, emptyList(), error = e.message ?: "Shell unavailable")
      }
    if (raw.contains("@MISSING@")) {
      return LogResult(source, emptyList(), missing = true)
    }
    val lines =
      raw
        .lineSequence()
        .map { it.trimEnd() }
        .filter { it.isNotBlank() }
        .toList()
        .takeLast(maxLines)
    val denied =
      lines.firstOrNull {
        it.contains("Operation not permitted", true) ||
          it.contains("Permission denied", true) ||
          it.contains("not found", true) && it.contains("dmesg", true)
      }
    return if (denied != null && lines.size <= 2) {
      LogResult(source, emptyList(), error = denied)
    } else {
      LogResult(source, lines)
    }
  }
}
