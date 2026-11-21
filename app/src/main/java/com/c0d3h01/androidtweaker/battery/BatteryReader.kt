package com.c0d3h01.androidtweaker.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.os.SystemClock
import com.c0d3h01.androidtweaker.common.normalizeTempC
import java.io.File
import kotlin.math.roundToInt

object BatteryReader {
  private val batteryDir: File? by lazy {
    listOf("/sys/class/power_supply/battery", "/sys/class/power_supply/bms")
      .map(::File)
      .firstOrNull { it.isDirectory }
  }

  fun read(context: Context): BatteryReading {
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val rawStatus = sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val plugged =
      when (sticky?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
        BatteryManager.BATTERY_PLUGGED_AC -> Plugged.AC
        BatteryManager.BATTERY_PLUGGED_USB -> Plugged.USB
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> Plugged.WIRELESS
        BatteryManager.BATTERY_PLUGGED_DOCK -> Plugged.DOCK
        else -> Plugged.NONE
      }
    val status =
      when (rawStatus) {
        BatteryManager.BATTERY_STATUS_CHARGING -> BatteryStatus.CHARGING
        BatteryManager.BATTERY_STATUS_DISCHARGING -> BatteryStatus.DISCHARGING
        BatteryManager.BATTERY_STATUS_FULL -> BatteryStatus.FULL
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryStatus.IDLE
        else -> BatteryStatus.UNKNOWN
      }
    val level =
      (bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 })
        ?: sticky
          ?.let { intent ->
            val raw = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (raw >= 0 && scale > 0) (raw * 100f / scale).roundToInt() else null
          }
    val current =
      batteryCurrentMa(bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW))
        ?: sysfsLong("current_now")?.let { (it / 1000.0).roundToInt() }
    val voltage =
      sticky?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)?.takeIf { it > 1_000 }
        ?: sysfsLong("voltage_now")?.let { (it / 1000).toInt() }?.takeIf { it > 1_000 }
    val temp =
      sticky
        ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        ?.takeUnless { it == Int.MIN_VALUE }
        ?.let { it / 10f }
        ?: sysfsLong("temp")?.let { normalizeTempC(it) }
    val counter =
      bm
        ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        ?.takeIf { it > 0 }
        ?.toLong()
        ?: sysfsLong("charge_counter")?.takeIf { it > 0 }
    val design = designCapacity(context)
    val full = sysfsLong("charge_full")?.toMahCapacity() ?: sysfsLong("energy_full")?.toEnergyCapacity(voltage)
    val cycles = sysfsLong("cycle_count")?.toInt()?.takeIf { it > 0 }
    val screen =
      (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isInteractive ?: true
    return BatteryReading(
      at = System.currentTimeMillis(),
      level = level,
      status = status,
      plugged = plugged,
      currentMa = current,
      voltageMv = voltage,
      tempC = temp,
      chargeCounterUah = counter,
      designMah = design,
      fullMah = full?.takeIf { it > 0 },
      cycles = cycles,
      health = sysfsText("health"),
      screenOn = screen,
    )
  }

  /** Design capacity from the kernel, the framework, or a common fallback. */
  fun designCapacity(context: Context): Int? {
    sysfsLong("charge_full_design")?.toMahCapacity()?.let { return it }
    sysfsLong("energy_full_design")?.let { raw ->
      val voltage = sysfsLong("voltage_now")?.let { if (it > 10_000) it / 1000 else it }
      voltage?.let { mv -> (raw / mv).toInt().takeIf { it in 1_000..20_000 } }?.let { return it }
    }
    runCatching {
      val id = context.resources.getIdentifier("config_batteryCapacity", "integer", "android")
      if (id != 0) context.resources.getInteger(id).takeIf { it in 1_000..20_000 } else null
    }.getOrNull()?.let { return it }
    return null
  }

  /** Kernel counters come as mAh or µAh depending on the driver; both fit here. */
  private fun Long.toMahCapacity(): Int? =
    when {
      this <= 0 -> null
      this <= 20_000 -> toInt()
      else -> (this / 1000).toInt()
    }?.takeIf { it in 1_000..20_000 }

  /** `energy_full*` is µWh (mWh or Wh on a few drivers); µWh / mV = mAh. */
  private fun Long.toEnergyCapacity(voltageMv: Int?): Int? {
    if (this <= 0) return null
    val mv = voltageMv ?: 3_800
    val uwh =
      when {
        this < 1_000 -> this * 1_000_000
        this < 1_000_000 -> this * 1_000
        else -> this
      }
    return (uwh / mv).toInt().takeIf { it in 1_000..20_000 }
  }

  private fun sysfsLong(name: String): Long? = sysfsText(name)?.toLongOrNull()

  private fun sysfsText(name: String): String? =
    batteryDir?.resolve(name)?.let { file ->
      runCatching { file.readText().trim().ifBlank { null } }.getOrNull()
    }
}

internal fun batteryCurrentMa(microamps: Int?): Int? =
  microamps?.takeUnless { it == Int.MIN_VALUE }?.let { (it / 1000.0).roundToInt() }

fun deepSleepNow(): Long = (SystemClock.elapsedRealtime() - SystemClock.uptimeMillis()).coerceAtLeast(0)
