package com.c0d3h01.androidtweaker.battery.analysis

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Charging quality: what share of this charge was easy on the battery. High end
 * level, high temperature and extreme power each subtract points. Pure so it can
 * be unit-tested.
 */
fun chargeQuality(
  startLevel: Int,
  endLevel: Int,
  peakTempC: Float?,
  peakPowerW: Float?,
): Int {
  var score = 100f
  score -= max(0, endLevel - 80) * 1.5f
  score -= max(0f, (peakTempC ?: 35f) - 35f) * 3f
  score -= max(0f, (peakPowerW ?: 0f) - 25f) * 0.4f
  if (endLevel - startLevel < 20) score += 4f
  return score.roundToInt().coerceIn(0, 100)
}

fun qualityTier(score: Int): String =
  when {
    score >= 80 -> "Gentle"
    score >= 55 -> "Standard"
    else -> "Stressful"
  }
