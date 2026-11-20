package com.c0d3h01.androidtweaker.battery.analysis

import com.c0d3h01.androidtweaker.battery.BatterySession
import com.c0d3h01.androidtweaker.battery.ScreenStats

fun screenStatsOf(sessions: List<BatterySession>): ScreenStats =
  ScreenStats(
    screenOnMs = sessions.filter { !it.charging }.sumOf { it.screenOnMs },
    screenOffMs = sessions.filter { !it.charging }.sumOf { it.screenOffMs },
    awakeMs = sessions.filter { !it.charging }.sumOf { (it.screenOffMs - it.deepSleepMs).coerceAtLeast(0) },
    deepSleepMs = sessions.filter { !it.charging }.sumOf { it.deepSleepMs },
  )
