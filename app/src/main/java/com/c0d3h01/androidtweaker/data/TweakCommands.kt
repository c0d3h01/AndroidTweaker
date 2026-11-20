package com.c0d3h01.androidtweaker.data

import java.util.Locale

/** Pure shell-command builders for the on-device tweak engine. */
object TweakCommands {
  const val EXIT_MARKER = "__AT_EXIT:"

  data class Parsed(
    val exitCode: Int,
    val output: String,
  )

  fun apply(dir: String, profile: Profile, stamp: String): String =
    run(dir, "apply ${profile.name.lowercase(Locale.US)}", stamp)

  fun misc(dir: String, stamp: String): String = run(dir, "misc", stamp)

  /** Any dispatcher command, e.g. "charge" or "clean cache". */
  fun run(dir: String, args: String, stamp: String): String = invoke(dir, args, stamp)

  fun parse(raw: String): Parsed {
    val at = raw.lastIndexOf(EXIT_MARKER)
    if (at < 0) return Parsed(-1, raw.trimEnd())
    val code = raw.substring(at + EXIT_MARKER.length).trim().lineSequence().firstOrNull()?.toIntOrNull() ?: -1
    return Parsed(code, raw.substring(0, at).trimEnd())
  }

  fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

  private fun invoke(dir: String, args: String, stamp: String): String {
    val script = quote("$dir/lib/dispatcher.sh")
    val log = quote("$dir/tweaks.log")
    val arguments = args.split(' ').filter(String::isNotEmpty).joinToString(" ", transform = ::quote)
    return buildString {
      append("printf '\\n=== %s %s ===\\n' ")
      append(quote(stamp))
      append(" ")
      append(quote(args))
      append(" >> ")
      append(log)
      append(" 2>/dev/null; ")
      append("MODDIR=")
      append(quote(dir))
      append(" sh ")
      append(script)
      append(" ")
      append(arguments)
      append(" 2>&1; __at=${'$'}?; echo \"")
      append(EXIT_MARKER)
      append("${'$'}__at\"")
    }
  }
}
