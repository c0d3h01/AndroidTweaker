package com.c0d3h01.androidtweaker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.c0d3h01.androidtweaker.battery.BatteryReader
import com.c0d3h01.androidtweaker.battery.BatteryReading
import com.c0d3h01.androidtweaker.battery.formatPower
import com.c0d3h01.androidtweaker.battery.formatTemp

/** Home-screen dial: level, state, temperature and live power. */
class BatteryWidgetProvider : AppWidgetProvider() {
  override fun onUpdate(
    context: Context,
    manager: AppWidgetManager,
    ids: IntArray,
  ) {
    val reading = runCatching { BatteryReader.read(context) }.getOrNull()
    ids.forEach { update(context, manager, it, reading) }
  }

  companion object {
    fun push(
      context: Context,
      reading: BatteryReading,
    ) {
      val manager = AppWidgetManager.getInstance(context) ?: return
      val ids = manager.getAppWidgetIds(ComponentName(context, BatteryWidgetProvider::class.java))
      ids.forEach { update(context, manager, it, reading) }
    }

    private fun update(
      context: Context,
      manager: AppWidgetManager,
      id: Int,
      reading: BatteryReading?,
    ) {
      val level = reading?.level
      val status =
        context.getString(
          when {
            reading == null -> R.string.status_reading
            reading.isCharging -> R.string.status_charging
            reading.isPlugged -> R.string.status_plugged
            else -> R.string.status_on_battery
          },
        )
      val views = RemoteViews(context.packageName, R.layout.widget_battery)
      views.setTextViewText(
        R.id.widget_level,
        level?.let { "$it%" } ?: context.getString(R.string.widget_battery_level_placeholder),
      )
      views.setTextViewText(R.id.widget_status, status)
      views.setTextViewText(R.id.widget_temp, formatTemp(reading?.tempC))
      views.setTextViewText(R.id.widget_power, formatPower(reading?.powerW))
      views.setProgressBar(R.id.widget_progress, 100, level ?: 0, level == null)
      views.setContentDescription(
        R.id.widget_root,
        if (reading == null) {
          context.getString(R.string.widget_battery_content_desc_unavailable)
        } else {
          val levelText =
            level?.let { context.getString(R.string.status_level_percent, it) }
              ?: context.getString(R.string.status_level_unknown)
          context.getString(
            R.string.widget_battery_content_desc,
            levelText,
            status,
            formatTemp(reading.tempC),
            formatPower(reading.powerW),
          )
        },
      )
      val open =
        PendingIntent.getActivity(
          context,
          0,
          MainActivity.launchIntent(context, MainActivity.SCREEN_BATTERY),
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
      views.setOnClickPendingIntent(R.id.widget_root, open)
      manager.updateAppWidget(id, views)
    }
  }
}
