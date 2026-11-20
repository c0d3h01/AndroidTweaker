package com.c0d3h01.androidtweaker.util

import com.c0d3h01.androidtweaker.data.ShellRunner

fun ShellRunner.isRoot(): Boolean = run("id -u").trim() == "0"
