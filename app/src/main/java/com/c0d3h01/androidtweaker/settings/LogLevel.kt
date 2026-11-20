package com.c0d3h01.androidtweaker.settings

enum class LogLevel { ERROR, WARN, OK, INFO }

private val errorWords = listOf("error", "fail", "denied", "panic", "oops", "fatal", "exception", "segfault")

private val warnWords = listOf("warn", "throttl", "timeout", "retry", "deprecated", "slow")

private val okWords = listOf("success", "granted", "mounted", "started", "enabled", " applied", " ok")

fun classifyLogLine(line: String): LogLevel {
  val s = line.lowercase()
  return when {
    errorWords.any { s.contains(it) } -> LogLevel.ERROR
    warnWords.any { s.contains(it) } -> LogLevel.WARN
    okWords.any { s.contains(it) } -> LogLevel.OK
    else -> LogLevel.INFO
  }
}
