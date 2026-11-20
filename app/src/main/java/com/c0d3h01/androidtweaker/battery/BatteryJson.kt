package com.c0d3h01.androidtweaker.battery

import org.json.JSONArray
import org.json.JSONObject

/** Compatibility codec for the existing battery preference and backup formats. */
internal object BatteryJson {
  fun settings(raw: String?): BatterySettings {
    if (raw == null) return BatterySettings()
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

  fun samples(raw: String?): MutableList<BatterySample> {
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
    return list
  }

  fun sessions(raw: String?): MutableList<BatterySession> {
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
    return list
  }

  fun samplesJson(list: List<BatterySample>): JSONArray {
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

  fun sessionsJson(list: List<BatterySession>): JSONArray {
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

  fun BatterySettings.toJson(): JSONObject =
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
}
