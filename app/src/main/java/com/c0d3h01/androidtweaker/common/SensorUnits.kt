package com.c0d3h01.androidtweaker.common

/**
 * Converts a raw thermal-zone reading to Celsius. Kernels report millidegrees
 * most of the time, but a few legacy drivers report deci-degrees or degrees.
 */
fun normalizeTempC(raw: Long): Float? {
  val c =
    when {
      raw > 200_000 -> return null
      raw >= 1_000 -> raw / 1000f
      raw >= 200 -> raw / 10f
      raw >= 20 -> raw.toFloat()
      raw >= 0 -> return null
      else -> return null
    }
  return if (c in -20f..150f) c else null
}
