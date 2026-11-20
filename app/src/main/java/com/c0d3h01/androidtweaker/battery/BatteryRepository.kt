package com.c0d3h01.androidtweaker.battery

import android.content.Context
import android.content.SharedPreferences

/**
 * Persistence for the battery monitor. Samples are a 24-hour ring buffer used
 * for the temperature chart and drain rate; sessions are the charging and
 * discharging history used for health and summaries.
 */
object BatteryRepository {
  private const val PREFS = "battery"
  private const val KEY_SAMPLES = "samples"
  private const val KEY_SESSIONS = "sessions"
  private const val KEY_SETTINGS = "settings"
  private const val KEY_OVERIGHT_DAY = "overnight_day"
  private const val KEY_WEEKLY_WEEK = "weekly_week"
  private val lock = Any()
  private val settingsLock = Any()
  private var samplesCache: MutableList<BatterySample>? = null
  private var sessionsCache: MutableList<BatterySession>? = null

  fun settings(context: Context): BatterySettings =
    BatteryJson.settings(prefs(context).getString(KEY_SETTINGS, null))

  fun save(
    context: Context,
    settings: BatterySettings,
  ) {
    prefs(context).edit().putString(KEY_SETTINGS, BatteryJson.run { settings.toJson() }.toString()).apply()
  }

  fun update(
    context: Context,
    change: (BatterySettings) -> BatterySettings,
  ): BatterySettings = synchronized(settingsLock) {
    val current = settings(context)
    val next = change(current)
    if (next != current) save(context, next)
    next
  }

  fun observeSettings(context: Context, onChange: () -> Unit): () -> Unit {
    val preferences = prefs(context)
    val listener =
      SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_SETTINGS || key == null) onChange()
      }
    preferences.registerOnSharedPreferenceChangeListener(listener)
    return { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
  }

  fun samples(context: Context): List<BatterySample> = synchronized(lock) { loadSamples(context).toList() }

  fun addSample(
    context: Context,
    sample: BatterySample,
    persist: Boolean = true,
  ) {
    synchronized(lock) {
      val list = loadSamples(context)
      val last = list.lastOrNull()
      if (last != null) {
        val elapsed = sample.at - last.at
        if (elapsed <= 0L || (elapsed < 20_000L && last.charging == sample.charging && last.screenOn == sample.screenOn)) return
      }
      list.add(sample)
      val cutoff = sample.at - 24 * 3_600_000L
      while (list.isNotEmpty() && list.first().at < cutoff) list.removeAt(0)
      while (list.size > 2_000) list.removeAt(0)
      if (persist) persistSamples(context, list)
    }
  }

  fun sessions(context: Context): List<BatterySession> = synchronized(lock) { loadSessions(context).toList() }

  fun addSession(
    context: Context,
    session: BatterySession,
  ) {
    synchronized(lock) {
      val list = loadSessions(context)
      if (list.lastOrNull()?.start == session.start && list.lastOrNull()?.end == session.end) return
      list.add(session)
      while (list.size > 400) list.removeAt(0)
      prefs(context).edit().putString(KEY_SESSIONS, BatteryJson.sessionsJson(list).toString()).apply()
    }
  }

  fun deleteHistory(context: Context) {
    synchronized(lock) {
      samplesCache = mutableListOf()
      sessionsCache = mutableListOf()
      prefs(context)
        .edit()
        .putString(KEY_SAMPLES, "[]")
        .putString(KEY_SESSIONS, "[]")
        .apply()
    }
  }

  fun lastOvernightDay(context: Context): Long? = prefs(context).getLong(KEY_OVERIGHT_DAY, 0L).takeIf { it > 0 }

  fun markOvernightDay(
    context: Context,
    day: Long,
  ) {
    prefs(context).edit().putLong(KEY_OVERIGHT_DAY, day).apply()
  }

  fun lastWeeklyWeek(context: Context): String? = prefs(context).getString(KEY_WEEKLY_WEEK, null)

  fun markWeeklyWeek(
    context: Context,
    week: String,
  ) {
    prefs(context).edit().putString(KEY_WEEKLY_WEEK, week).apply()
  }

  fun summarySince(
    context: Context,
    since: Long,
  ): BatterySummary {
    val list = sessions(context).filter { it.end >= since }
    val charge = list.filter { it.charging }
    val discharge = list.filter { !it.charging }
    return BatterySummary(
      chargeCount = charge.size,
      addedMah = charge.sumOf { it.addedMah },
      averageQuality = if (charge.isEmpty()) null else charge.map { it.quality }.average().toInt(),
      screenOnMs = discharge.sumOf { it.screenOnMs },
      deepSleepMs = discharge.sumOf { it.deepSleepMs },
      drainPercent = discharge.sumOf { (it.startLevel - it.endLevel).coerceAtLeast(0) },
    )
  }

  private fun loadSamples(context: Context): MutableList<BatterySample> {
    samplesCache?.let { return it }
    return BatteryJson.samples(prefs(context).getString(KEY_SAMPLES, null)).also { samplesCache = it }
  }

  private fun loadSessions(context: Context): MutableList<BatterySession> {
    sessionsCache?.let { return it }
    return BatteryJson.sessions(prefs(context).getString(KEY_SESSIONS, null)).also { sessionsCache = it }
  }

  private fun persistSamples(
    context: Context,
    list: List<BatterySample>,
  ) {
    prefs(context).edit().putString(KEY_SAMPLES, BatteryJson.samplesJson(list).toString()).apply()
  }

  private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

data class BatterySummary(
  val chargeCount: Int,
  val addedMah: Int,
  val averageQuality: Int?,
  val screenOnMs: Long,
  val deepSleepMs: Long,
  val drainPercent: Int,
)
