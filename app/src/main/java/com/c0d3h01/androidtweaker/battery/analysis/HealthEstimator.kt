package com.c0d3h01.androidtweaker.battery.analysis

import com.c0d3h01.androidtweaker.battery.BatterySession
import com.c0d3h01.androidtweaker.battery.HealthEstimate
import kotlin.math.roundToInt

/** Confidence and capacity from charge sessions; phone-reported capacity wins when plausible. */
fun estimateHealth(
  sessions: List<BatterySession>,
  designMah: Int?,
  phoneFullMah: Int?,
  cycles: Int?,
): HealthEstimate {
  val usable =
    sessions.filter {
      it.charging && it.levelDelta >= 15 && it.addedMah > 0 && it.durationMs >= 5 * 60_000
    }
  val design = designMah
  val phoneCapacity =
    phoneFullMah?.takeIf { full ->
      design == null || full.toFloat() / design in 0.40f..1.30f
    }
  if (phoneCapacity != null) {
    val percent = design?.let { (phoneCapacity * 100f / it).roundToInt().coerceIn(0, 130) }
    return HealthEstimate(
      capacityMah = phoneCapacity,
      designMah = design,
      percent = percent,
      confidence = if (usable.size >= 2) "Verified" else "Estimated",
      sessionsUsed = usable.size,
      cycles = cycles,
    )
  }
  if (usable.isEmpty() || design == null) {
    return HealthEstimate(null, design, null, "Learning", usable.size, cycles)
  }
  val measured = usable.mapNotNull { it.capacityMah }.filter { it.toFloat() / design in 0.4f..1.3f }
  if (measured.isEmpty()) return HealthEstimate(null, design, null, "Learning", 0, cycles)
  val sorted = measured.sorted()
  val median = sorted[sorted.size / 2]
  val spread = (sorted.last() - sorted.first()).toFloat() / median
  val confidence =
    when {
      measured.size < 2 -> "Learning"
      measured.size < 4 -> "Approximate"
      spread <= 0.04f -> "Verified"
      else -> "Estimated"
    }
  return HealthEstimate(
    capacityMah = median,
    designMah = design,
    percent = (median * 100f / design).roundToInt().coerceIn(0, 130),
    confidence = confidence,
    sessionsUsed = measured.size,
    cycles = cycles,
  )
}
