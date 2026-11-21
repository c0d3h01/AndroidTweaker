package com.c0d3h01.androidtweaker.battery.monitor

import android.content.Context
import com.c0d3h01.androidtweaker.battery.BatteryReading
import com.c0d3h01.androidtweaker.battery.BatteryRepository
import com.c0d3h01.androidtweaker.battery.BatterySession
import com.c0d3h01.androidtweaker.battery.BatterySettings
import com.c0d3h01.androidtweaker.battery.analysis.drainRate
import com.c0d3h01.androidtweaker.battery.analysis.qualityTier
import com.c0d3h01.androidtweaker.battery.formatDuration
import com.c0d3h01.androidtweaker.battery.formatMah
import com.c0d3h01.androidtweaker.battery.formatPower
import com.c0d3h01.androidtweaker.battery.formatTemp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.WeekFields

/** Opportunistic report checks; this is not an exact background scheduler. */
internal class BatteryReportPolicy(
  private val context: Context,
  private val notifications: BatteryNotifications,
) {
  private var lastAnomalyAt = 0L

  fun evaluate(
    reading: BatteryReading,
    settings: BatterySettings,
  ) {
    val now = LocalDateTime.now()
    if (settings.overnightReport && now.hour in 6..10) {
      val today = LocalDate.now().toEpochDay()
      if (BatteryRepository.lastOvernightDay(context) != today) {
        BatteryRepository.markOvernightDay(context, today)
        overnightReport()
      }
    }
    if (settings.weeklySummary && now.dayOfWeek.value == 1 && now.hour >= 8) {
      val week =
        LocalDate.now().get(WeekFields.ISO.weekBasedYear()).toString() + "-" +
          LocalDate.now().get(WeekFields.ISO.weekOfWeekBasedYear())
      if (BatteryRepository.lastWeeklyWeek(context) != week) {
        BatteryRepository.markWeeklyWeek(context, week)
        weeklyReport()
      }
    }
    if (settings.anomalyAlert && !reading.isPlugged && !reading.screenOn) {
      val nowMs = System.currentTimeMillis()
      if (nowMs - lastAnomalyAt >= 6 * 3_600_000L) {
        val rate = drainRate(BatteryRepository.samples(context), false, nowMs) ?: 0f
        if (rate >= 8f) {
          lastAnomalyAt = nowMs
          notifications.alarm("anomaly", "Unusual battery drain", "%.1f%%/h with the screen off".format(rate))
        }
      }
    }
  }

  private fun overnightReport() {
    val samples = BatteryRepository.samples(context)
    val end = System.currentTimeMillis()
    val points = samples.filter { it.at >= end - 9 * 3_600_000L && !it.charging }
    if (points.size < 3) return
    val first = points.first()
    val last = points.last()
    val hours = (last.at - first.at) / 3_600_000f
    if (hours < 2f) return
    val drop = first.level - last.level
    if (drop <= 0) return
    val rate = drop / hours
    notifications.summary(
      "overnight",
      "Overnight report",
      "Battery dropped $drop% · %.1f%%/h".format(rate),
      "From ${first.level}% to ${last.level}% over ${formatDuration(
        last.at - first.at,
      )}. Deep sleep and screen-off drain are on the Battery screen.",
    )
  }

  private fun weeklyReport() {
    val week = BatteryRepository.summarySince(context, System.currentTimeMillis() - 7 * 24 * 3_600_000L)
    if (week.chargeCount == 0 && week.drainPercent == 0) return
    notifications.summary(
      "weekly",
      "Weekly battery report",
      "${week.drainPercent}% drained · ${week.chargeCount} charges",
      "Added ${formatMah(week.addedMah)} · screen on ${formatDuration(week.screenOnMs)} · deep sleep ${formatDuration(week.deepSleepMs)}" +
        (week.averageQuality?.let { " · charge quality $it" } ?: ""),
    )
  }

  fun chargingSummary(session: BatterySession) {
    val settings = BatteryRepository.settings(context)
    if (!settings.chargingSummary) return
    val delta = (session.endLevel - session.startLevel).coerceAtLeast(0)
    val rate = if (session.durationMs > 0) delta / (session.durationMs / 3_600_000f) else 0f
    val title = "⚡ Charged ${session.startLevel}% → ${session.endLevel}% in ${formatDuration(session.durationMs)}"
    val body =
      "+$delta% · ${formatMah(session.addedMah)} · ${"%.0f".format(rate)}%/h\n" +
        "${formatPower(session.peakPowerW)} peak power · ${formatTemp(session.peakTempC)} peak temp" +
        (if (session.quality > 0) " · quality ${session.quality} (${qualityTier(session.quality)})" else "")
    notifications.summary("charging-summary", title, "Charging summary", body)
  }

}
