package com.c0d3h01.androidtweaker.access

/** Minimal command runner used by the root shell and by tests. */
fun interface CommandRunner {
  fun run(cmd: String): String
}

/** Validate the backend UID before accepting a privileged session. */
fun CommandRunner.isRoot(): Boolean = run("id -u").trim() == "0"
