package com.c0d3h01.androidtweaker.battery.analysis

import com.c0d3h01.androidtweaker.battery.BatteryReading
import com.c0d3h01.androidtweaker.battery.BatterySample
import kotlin.math.abs

/** Drain in percent per hour over a window of samples, split by screen state. */
fun drainRate(
  samples: List<BatterySample>,
  screenOn: Boolean,
  now: Long,
): Float? {
  val points =
    samples.filter {
      !it.charging && it.screenOn == screenOn && it.level in 0..100 && now - it.at <= 6 * 3_600_000L
    }
  if (points.size < 3) return null
  val first = points.first()
  val last = points.last()
  val hours = (last.at - first.at) / 3_600_000f
  if (hours < 0.15f) return null
  val delta = first.level - last.level
  if (delta < 0) return null
  return (delta / hours).coerceIn(0f, 60f)
}

/** Estimated time left with the screen on, from measured drain or live current. */
fun estimateRemaining(
  samples: List<BatterySample>,
  reading: BatteryReading,
  now: Long,
): Long? {
  val level = reading.level ?: return null
  if (reading.isPlugged) return null
  val rate = drainRate(samples, reading.screenOn, now)
  if (rate != null && rate > 0.2f) return (level / rate * 3_600_000L).toLong()
  val current = abs(reading.currentMa ?: return null)
  val capacity = reading.capacityMah ?: return null
  if (current < 20) return null
  return ((capacity * level / 100f) / current * 3_600_000L).toLong()
}

fun estimateToFull(reading: BatteryReading): Long? {
  val level = reading.level ?: return null
  if (!reading.isPlugged || level >= 100) return null
  val current = abs(reading.currentMa ?: return null)
  val capacity = reading.capacityMah ?: return null
  if (current < 20) return null
  return ((capacity * (100 - level) / 100f) / current * 3_600_000L).toLong()
}
