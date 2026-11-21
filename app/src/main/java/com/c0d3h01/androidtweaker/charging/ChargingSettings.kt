package com.c0d3h01.androidtweaker.charging

/** Software charge holds; disabled until the user opts in. */
data class ChargingSettings(
  val chargeLimit: Int = 0,
  val chargeBypass: Boolean = false,
  val tempFuse: Int = 0,
) {
  val active: Boolean get() = chargeLimit > 0 || chargeBypass || tempFuse > 0

  fun sanitized(): ChargingSettings = copy(
    chargeLimit = if (chargeLimit == 0) 0 else chargeLimit.coerceIn(50..100),
    tempFuse = if (tempFuse == 0) 0 else tempFuse.coerceIn(35..60),
  )
}
