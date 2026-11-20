package com.c0d3h01.androidtweaker.battery

import android.content.Context
import java.io.File
import java.util.Locale
import org.json.JSONObject

/** Creates local share files; never sends them. */
object BatteryExport {
  fun exportCsv(context: Context): File {
    val file = File(context.cacheDir, "androidtweaker-export.csv")
    file.bufferedWriter().use { out ->
      out.appendLine(
        "kind,start,end,charging,start_level,end_level,added_mah,peak_power_w,peak_temp_c,quality,screen_on_ms,screen_off_ms,deep_sleep_ms",
      )
      BatteryRepository.sessions(context).forEach { s ->
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
      BatteryRepository.samples(context).forEach { p ->
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
        .put("settings", BatteryJson.run { BatteryRepository.settings(context).toJson() })
        .put("samples", BatteryJson.samplesJson(BatteryRepository.samples(context)))
        .put("sessions", BatteryJson.sessionsJson(BatteryRepository.sessions(context)))
    file.writeText(root.toString())
    return file
  }
}
