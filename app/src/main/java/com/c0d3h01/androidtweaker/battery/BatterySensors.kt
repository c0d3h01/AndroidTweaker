package com.c0d3h01.androidtweaker.battery

import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

/** Direct readable battery sources; never requests a privileged shell. */
internal class BatterySensors(
  private val read: (String) -> String? = { path -> runCatching { File(path).readText() }.getOrNull() },
) {
  private val supplies = listOf("/sys/class/power_supply/battery", "/sys/class/power_supply/bms")

  fun text(name: String): String? = supplies.asSequence()
    .mapNotNull { read("$it/$name")?.trim()?.takeIf(String::isNotEmpty) }
    .firstOrNull()

  fun longs(name: String): List<Long> = supplies.mapNotNull { read("$it/$name")?.trim()?.toLongOrNull() }
}

/** All sources use microamps; zero instantaneous values must not hide a usable fallback. */
internal fun resolveBatteryCurrentMa(
  frameworkNow: Int?,
  sysfsNow: List<Long>,
  frameworkAverage: Int?,
  allowZero: Boolean,
): Int? {
  val candidates = listOfNotNull(batteryCurrentMa(frameworkNow)) +
    sysfsNow.filter { it in -100_000_000L..100_000_000L }
      .map { (it / 1000.0).roundToInt() } +
    listOfNotNull(batteryCurrentMa(frameworkAverage))
  return candidates.firstOrNull { it != 0 } ?: if (allowZero) candidates.firstOrNull() else null
}

internal fun batteryPowerWatts(currentMa: Int?, voltageMv: Int?): Float? {
  val current = currentMa ?: return null
  val voltage = voltageMv?.takeIf { it > 0 } ?: return null
  return abs(current.toDouble()).times(voltage).div(1_000_000).toFloat()
}

/** Broadcast voltage is mV; kernel voltage_now uses microvolts. */
internal fun resolveBatteryVoltageMv(broadcastMv: Int?, sysfsMicrovolts: List<Long>): Int? =
  broadcastMv?.takeIf { it > 1_000 }
    ?: sysfsMicrovolts.asSequence().map { it / 1000 }.firstOrNull { it in 1_001..100_000 }?.toInt()
