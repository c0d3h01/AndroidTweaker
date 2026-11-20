package com.c0d3h01.androidtweaker.data

/** Minimal command runner used by the root shell and by tests. */
fun interface ShellRunner {
  fun run(cmd: String): String
}
