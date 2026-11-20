package com.c0d3h01.androidtweaker.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

/** Settings shown on the Battery screen; every alarm can be disabled on its own. */
data class BatterySettings(
  val monitorEnabled: Boolean = false,
  val refreshSeconds: Int = 5,
  val iconStyle: Int = ICON_PERCENT_TEMP,
  val notifyFull: Boolean = true,
  val notifyLow: Boolean = true,
  val lowLevel: Int = 15,
  val notifyLevel: Boolean = false,
  val levelAlarm: Int = 80,
  val notifyHighTemp: Boolean = true,
  val highTempC: Int = 45,
  val notifyLowTemp: Boolean = false,
  val lowTempC: Int = 5,
  val chargingSummary: Boolean = true,
  val overnightReport: Boolean = true,
  val weeklySummary: Boolean = true,
  val anomalyAlert: Boolean = false,
) {
  companion object {
    const val ICON_OFF = 0
    const val ICON_PERCENT = 1
    const val ICON_PERCENT_TEMP = 2
    const val ICON_RING = 3
  }
}

/**
 * Persistence for the battery monitor. Samples are a 24-hour ring buffer used
 * for the temperature chart and drain rate; sessions are the charging and
 * discharging history used for health and summaries.
 */
object BatteryStore {
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

  fun settings(context: Context): BatterySettings {
    val raw = prefs(context).getString(KEY_SETTINGS, null) ?: return BatterySettings()
    return runCatching {
      val o = JSONObject(raw)
      BatterySettings(
        monitorEnabled = o.optBoolean("monitorEnabled", false),
        refreshSeconds = o.optInt("refreshSeconds", 5).coerceIn(2, 60),
        iconStyle = o.optInt("iconStyle", BatterySettings.ICON_PERCENT_TEMP),
        notifyFull = o.optBoolean("notifyFull", true),
        notifyLow = o.optBoolean("notifyLow", true),
        lowLevel = o.optInt("lowLevel", 15).coerceIn(2, 50),
        notifyLevel = o.optBoolean("notifyLevel", false),
        levelAlarm = o.optInt("levelAlarm", 80).coerceIn(5, 100),
        notifyHighTemp = o.optBoolean("notifyHighTemp", true),
        highTempC = o.optInt("highTempC", 45).coerceIn(35, 70),
        notifyLowTemp = o.optBoolean("notifyLowTemp", false),
        lowTempC = o.optInt("lowTempC", 5).coerceIn(-20, 20),
        chargingSummary = o.optBoolean("chargingSummary", true),
        overnightReport = o.optBoolean("overnightReport", true),
        weeklySummary = o.optBoolean("weeklySummary", true),
        anomalyAlert = o.optBoolean("anomalyAlert", false),
      )
    }.getOrDefault(BatterySettings())
  }

  fun save(
    context: Context,
    settings: BatterySettings,
  ) {
    prefs(context).edit().putString(KEY_SETTINGS, settings.toJson().toString()).apply()
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
      prefs(context).edit().putString(KEY_SESSIONS, sessionsJson(list).toString()).apply()
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

  /** CSV of samples and sessions in the app cache, ready to share. */
  fun exportCsv(context: Context): File {
    val file = File(context.cacheDir, "androidtweaker-export.csv")
    file.bufferedWriter().use { out ->
      out.appendLine(
        "kind,start,end,charging,start_level,end_level,added_mah,peak_power_w,peak_temp_c,quality,screen_on_ms,screen_off_ms,deep_sleep_ms",
      )
      sessions(context).forEach { s ->
        out.appendLine(
          listOf(
            "session",
            s.start,
            s.end,
            s.charging,
            s.startLevel,
            s.endLevel,
            s.addedMah,
            String.format(Locale.US, "%.2f", s.peakPowerW),
            s.peakTempC?.let { String.format(Locale.US, "%.1f", it) } ?: "",
            s.quality,
            s.screenOnMs,
            s.screenOffMs,
            s.deepSleepMs,
          ).joinToString(","),
        )
      }
      out.appendLine()
      out.appendLine("time,level,temp_c,current_ma,voltage_mv,charging,screen_on")
      samples(context).forEach { p ->
        out.appendLine(
          listOf(
            "sample",
            p.at,
            p.level,
            p.tempC?.let { String.format(Locale.US, "%.1f", it) } ?: "",
            p.currentMa ?: "",
            p.voltageMv ?: "",
            p.charging,
            p.screenOn,
          ).joinToString(","),
        )
      }
    }
    return file
  }

  fun backupJson(context: Context): File {
    val file = File(context.cacheDir, "androidtweaker-backup.json")
    val root =
      JSONObject()
        .put("version", 1)
        .put("created", System.currentTimeMillis())
        .put("settings", settings(context).toJson())
        .put("samples", samplesJson(samples(context)))
        .put("sessions", sessionsJson(sessions(context)))
    file.writeText(root.toString())
    return file
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
    val raw = prefs(context).getString(KEY_SAMPLES, null)
    val list = mutableListOf<BatterySample>()
    if (!raw.isNullOrBlank()) {
      runCatching {
        val array = JSONArray(raw)
        for (i in 0 until array.length()) {
          val o = array.getJSONObject(i)
          list.add(
            BatterySample(
              at = o.optLong("t"),
              level = o.optInt("l"),
              tempC = if (o.has("c")) o.optDouble("c").toFloat() else null,
              currentMa = if (o.has("i")) o.optInt("i") else null,
              voltageMv = if (o.has("v")) o.optInt("v") else null,
              charging = o.optBoolean("g"),
              screenOn = o.optBoolean("s"),
            ),
          )
        }
      }
    }
    samplesCache = list
    return list
  }

  private fun loadSessions(context: Context): MutableList<BatterySession> {
    sessionsCache?.let { return it }
    val raw = prefs(context).getString(KEY_SESSIONS, null)
    val list = mutableListOf<BatterySession>()
    if (!raw.isNullOrBlank()) {
      runCatching {
        val array = JSONArray(raw)
        for (i in 0 until array.length()) {
          val o = array.getJSONObject(i)
          list.add(
            BatterySession(
              start = o.optLong("a"),
              end = o.optLong("b"),
              charging = o.optBoolean("c"),
              startLevel = o.optInt("d"),
              endLevel = o.optInt("e"),
              addedMah = o.optInt("f"),
              peakPowerW = o.optDouble("g").toFloat(),
              peakTempC = if (o.has("h")) o.optDouble("h").toFloat() else null,
              charger = runCatching { Plugged.valueOf(o.optString("i", "NONE")) }.getOrDefault(Plugged.NONE),
              quality = o.optInt("j"),
              capacityMah = if (o.has("k")) o.optInt("k") else null,
              screenOnMs = o.optLong("l"),
              screenOffMs = o.optLong("m"),
              deepSleepMs = o.optLong("n"),
            ),
          )
        }
      }
    }
    sessionsCache = list
    return list
  }

  private fun persistSamples(
    context: Context,
    list: List<BatterySample>,
  ) {
    prefs(context).edit().putString(KEY_SAMPLES, samplesJson(list).toString()).apply()
  }

  private fun samplesJson(list: List<BatterySample>): JSONArray {
    val array = JSONArray()
    for (p in list) {
      val o = JSONObject()
      o.put("t", p.at)
      o.put("l", p.level)
      p.tempC?.let { o.put("c", it.toDouble()) }
      p.currentMa?.let { o.put("i", it) }
      p.voltageMv?.let { o.put("v", it) }
      o.put("g", p.charging)
      o.put("s", p.screenOn)
      array.put(o)
    }
    return array
  }

  private fun sessionsJson(list: List<BatterySession>): JSONArray {
    val array = JSONArray()
    for (s in list) {
      val o = JSONObject()
      o.put("a", s.start)
      o.put("b", s.end)
      o.put("c", s.charging)
      o.put("d", s.startLevel)
      o.put("e", s.endLevel)
      o.put("f", s.addedMah)
      o.put("g", s.peakPowerW.toDouble())
      s.peakTempC?.let { o.put("h", it.toDouble()) }
      o.put("i", s.charger.name)
      o.put("j", s.quality)
      s.capacityMah?.let { o.put("k", it) }
      o.put("l", s.screenOnMs)
      o.put("m", s.screenOffMs)
      o.put("n", s.deepSleepMs)
      array.put(o)
    }
    return array
  }

  private fun BatterySettings.toJson(): JSONObject =
    JSONObject()
      .put("monitorEnabled", monitorEnabled)
      .put("refreshSeconds", refreshSeconds)
      .put("iconStyle", iconStyle)
      .put("notifyFull", notifyFull)
      .put("notifyLow", notifyLow)
      .put("lowLevel", lowLevel)
      .put("notifyLevel", notifyLevel)
      .put("levelAlarm", levelAlarm)
      .put("notifyHighTemp", notifyHighTemp)
      .put("highTempC", highTempC)
      .put("notifyLowTemp", notifyLowTemp)
      .put("lowTempC", lowTempC)
      .put("chargingSummary", chargingSummary)
      .put("overnightReport", overnightReport)
      .put("weeklySummary", weeklySummary)
      .put("anomalyAlert", anomalyAlert)

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
