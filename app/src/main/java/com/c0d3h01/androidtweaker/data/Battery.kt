package com.c0d3h01.androidtweaker.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

enum class Plugged { NONE, AC, USB, WIRELESS, DOCK }

enum class BatteryStatus { CHARGING, DISCHARGING, FULL, IDLE, UNKNOWN }

/** One live reading, normalised across the many ways Android reports battery data. */
data class BatteryReading(
  val at: Long,
  val level: Int?,
  val status: BatteryStatus,
  val plugged: Plugged,
  val currentMa: Int?,
  val voltageMv: Int?,
  val tempC: Float?,
  val chargeCounterUah: Long?,
  val designMah: Int?,
  val fullMah: Int?,
  val cycles: Int?,
  val health: String?,
  val screenOn: Boolean,
) {
  val isPlugged: Boolean get() = plugged != Plugged.NONE
  val isCharging: Boolean get() = status == BatteryStatus.CHARGING
  val powerW: Float?
    get() {
      val ma = currentMa ?: return null
      val mv = voltageMv ?: return null
      return abs(ma) * mv / 1_000_000f
    }
  val capacityMah: Int? get() = fullMah?.takeIf { it > 0 } ?: designMah?.takeIf { it > 0 }
}

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

/** App usage for the current discharging session, when Usage access is granted. */
data class AppUsage(
  val packageName: String,
  val label: String,
  val screenMs: Long,
  val launches: Int,
  val share: Float,
)

object UsageReader {
  fun hasAccess(context: Context): Boolean {
    val ops = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
    val mode =
      if (Build.VERSION.SDK_INT >= 29) {
        ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
      } else {
        @Suppress("DEPRECATION")
        ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
      }
    return mode == AppOpsManager.MODE_ALLOWED
  }

  fun apps(
    context: Context,
    since: Long,
    limit: Int = 8,
  ): List<AppUsage> {
    if (!hasAccess(context)) return emptyList()
    val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyList()
    val now = System.currentTimeMillis()
    val stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, since, now).orEmpty()
    val grouped = linkedMapOf<String, Pair<Long, Int>>()
    for (stat in stats) {
      val previous = grouped[stat.packageName]
      grouped[stat.packageName] = (previous?.first ?: 0L) + stat.totalTimeInForeground to
        (previous?.second ?: 0) + stat.lastTimeUsed.let { if (it >= since) 1 else 0 }
    }
    val total = grouped.values.sumOf { it.first }.coerceAtLeast(1L)
    val pm = context.packageManager
    return grouped.entries
      .filter { it.value.first >= 30_000 }
      .sortedByDescending { it.value.first }
      .take(limit)
      .map { (pkg, value) ->
        val label =
          runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
          }.getOrDefault(pkg.substringAfterLast('.'))
        AppUsage(pkg, label, value.first, value.second, value.first.toFloat() / total)
      }
  }
}

data class HealthEstimate(
  val capacityMah: Int?,
  val designMah: Int?,
  val percent: Int?,
  val confidence: String,
  val sessionsUsed: Int,
  val cycles: Int?,
) {
  val label: String
    get() =
      when {
        percent == null -> "Not yet"
        percent >= 90 -> "Excellent"
        percent >= 80 -> "Good"
        percent >= 70 -> "OK"
        percent >= 60 -> "Fair"
        else -> "Bad"
      }
}

data class BatterySession(
  val start: Long,
  val end: Long,
  val charging: Boolean,
  val startLevel: Int,
  val endLevel: Int,
  val addedMah: Int,
  val peakPowerW: Float,
  val peakTempC: Float?,
  val charger: Plugged,
  val quality: Int,
  val capacityMah: Int?,
  val screenOnMs: Long,
  val screenOffMs: Long,
  val deepSleepMs: Long,
) {
  val durationMs: Long get() = (end - start).coerceAtLeast(0)
  val levelDelta: Int get() = (endLevel - startLevel)
}

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

data class ScreenStats(
  val screenOnMs: Long,
  val screenOffMs: Long,
  val awakeMs: Long,
  val deepSleepMs: Long,
)

fun screenStatsOf(sessions: List<BatterySession>): ScreenStats =
  ScreenStats(
    screenOnMs = sessions.filter { !it.charging }.sumOf { it.screenOnMs },
    screenOffMs = sessions.filter { !it.charging }.sumOf { it.screenOffMs },
    awakeMs = sessions.filter { !it.charging }.sumOf { (it.screenOffMs - it.deepSleepMs).coerceAtLeast(0) },
    deepSleepMs = sessions.filter { !it.charging }.sumOf { it.deepSleepMs },
  )

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

data class BatterySample(
  val at: Long,
  val level: Int,
  val tempC: Float?,
  val currentMa: Int?,
  val voltageMv: Int?,
  val charging: Boolean,
  val screenOn: Boolean,
)

fun formatDuration(ms: Long?): String {
  if (ms == null || ms < 0) return "—"
  val totalMinutes = ms / 60_000
  val hours = totalMinutes / 60
  val minutes = totalMinutes % 60
  return when {
    hours >= 24 -> String.format(Locale.US, "%dd\u00A0%dh", hours / 24, hours % 24)
    hours > 0 -> String.format(Locale.US, "%dh\u00A0%02dm", hours, minutes)
    else -> "${minutes}m"
  }
}

fun formatMah(mah: Int?): String = mah?.let { "$it\u00A0mAh" } ?: "—"

fun formatPower(w: Float?): String = w?.let { String.format(Locale.US, "%.1f\u00A0W", it) } ?: "—"

fun formatCurrent(ma: Int?): String = ma?.let { (if (it > 0) "+" else "") + String.format(Locale.US, "%d\u00A0mA", it) } ?: "—"

fun formatVoltage(mv: Int?): String = mv?.let { String.format(Locale.US, "%.2f\u00A0V", it / 1000f) } ?: "—"

fun formatTemp(c: Float?): String = c?.let { String.format(Locale.US, "%.1f\u00A0°C", it) } ?: "—"

fun chargeSpeedLabel(
  currentMa: Int?,
  powerW: Float?,
  plugged: Plugged,
): String =
  when {
    plugged == Plugged.NONE -> "Not charging"
    (powerW ?: 0f) >= 15f || (currentMa ?: 0) >= 3_000 -> "Super fast"
    (powerW ?: 0f) >= 7f || (currentMa ?: 0) >= 1_500 -> "Fast"
    (powerW ?: 0f) >= 2f || (currentMa ?: 0) >= 500 -> "Standard"
    else -> "Slow"
  }

fun plugLabel(plugged: Plugged): String =
  when (plugged) {
    Plugged.AC -> "AC charger"
    Plugged.USB -> "USB"
    Plugged.WIRELESS -> "Wireless charger"
    Plugged.DOCK -> "Dock"
    Plugged.NONE -> "Battery"
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

fun deepSleepNow(): Long = (SystemClock.elapsedRealtime() - SystemClock.uptimeMillis()).coerceAtLeast(0)
