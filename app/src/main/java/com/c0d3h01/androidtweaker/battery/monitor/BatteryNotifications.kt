package com.c0d3h01.androidtweaker.battery.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.media.RingtoneManager
import android.os.Build
import com.c0d3h01.androidtweaker.BatteryMonitorService
import com.c0d3h01.androidtweaker.MainActivity
import com.c0d3h01.androidtweaker.R
import com.c0d3h01.androidtweaker.battery.BatteryReading
import com.c0d3h01.androidtweaker.battery.BatteryRepository
import com.c0d3h01.androidtweaker.battery.BatterySettings
import com.c0d3h01.androidtweaker.battery.BatteryStatus
import com.c0d3h01.androidtweaker.battery.Plugged
import com.c0d3h01.androidtweaker.battery.analysis.drainRate
import com.c0d3h01.androidtweaker.battery.analysis.estimateToFull
import com.c0d3h01.androidtweaker.battery.formatCurrent
import com.c0d3h01.androidtweaker.battery.formatDuration
import com.c0d3h01.androidtweaker.battery.formatPower
import com.c0d3h01.androidtweaker.battery.formatTemp
import com.c0d3h01.androidtweaker.battery.formatVoltage
import kotlin.math.abs

/** Android rendering and delivery; policies decide when to notify. */
internal class BatteryNotifications(private val context: Context) {
  fun alarm(
    key: String,
    title: String,
    body: String,
  ) {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    val open = openIntent()
    val notification =
      Notification
        .Builder(context, CHANNEL_ALARMS)
        .setSmallIcon(R.drawable.ic_stat_battery)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(Notification.BigTextStyle().bigText(body))
        .setContentIntent(open)
        .setAutoCancel(true)
        .setCategory(Notification.CATEGORY_ALARM)
        .setVisibility(Notification.VISIBILITY_PUBLIC)
        .build()
    runCatching { manager.notify(ALARM_BASE_ID + (key.hashCode() and 0xFF), notification) }
  }

  fun summary(
    key: String,
    title: String,
    body: String,
    big: String,
  ) {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    val notification =
      Notification
        .Builder(context, CHANNEL_SUMMARY)
        .setSmallIcon(R.drawable.ic_stat_battery)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(Notification.BigTextStyle().bigText(big))
        .setContentIntent(openIntent())
        .setAutoCancel(true)
        .setCategory(Notification.CATEGORY_STATUS)
        .build()
    runCatching { manager.notify(SUMMARY_BASE_ID + (key.hashCode() and 0xFF), notification) }
  }

  fun notification(
    reading: BatteryReading,
    settings: BatterySettings,
  ): Notification {
    val level = reading.level
    val statusLabel =
      when {
        reading.status == BatteryStatus.FULL -> "Full"
        reading.isCharging -> "Charging"
        reading.isPlugged -> "Plugged in"
        else -> "On battery"
      }
    val headline = level?.let { "$it% · $statusLabel" } ?: statusLabel
    val line1 =
      listOfNotNull(
        reading.powerW?.let { formatPower(it) },
        formatVoltage(reading.voltageMv),
        formatTemp(reading.tempC),
      ).joinToString(" · ").ifBlank { "Reading battery…" }
    val line2 =
      when {
        reading.isCharging -> estimateToFull(reading)?.let { "${formatDuration(it)} to full" } ?: "Measuring charge speed…"
        reading.isPlugged -> "Plugged in · not charging"
        else -> estimateRemainingText(reading)
      }
    val big = "$line1\n$line2\n${formatCurrent(reading.currentMa)} · ${reading.chargerLabel()}"
    val builder =
      monitorNotificationBuilder()
        .setContentTitle(headline)
        .setContentText("$line1 · $line2")
        .setStyle(Notification.BigTextStyle().bigText(big))
        .setProgress(100, level ?: 0, level == null)
    if (settings.iconStyle == BatterySettings.ICON_OFF) {
      builder.setSmallIcon(R.drawable.ic_stat_battery)
    } else {
      builder.setSmallIcon(statusIcon(settings.iconStyle, level))
    }
    return builder.build()
  }

  fun monitorNotificationBuilder(): Notification.Builder =
    Notification
      .Builder(context, CHANNEL_MONITOR)
      .setContentIntent(openIntent())
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setShowWhen(false)
      .setCategory(Notification.CATEGORY_SERVICE)
      .setVisibility(Notification.VISIBILITY_PUBLIC)
      .addAction(action(context.getString(R.string.battery_monitor_pause), BatteryMonitorService.ACTION_STOP, 1))
      .apply {
        if (Build.VERSION.SDK_INT >= 31) {
          setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
      }

  private fun estimateRemainingText(reading: BatteryReading): String {
    val level = reading.level ?: return "Reading battery…"
    val rate = drainRate(BatteryRepository.samples(context), reading.screenOn, System.currentTimeMillis())
    if (rate != null && rate > 0.2f) {
      val hours = level / rate
      return "About ${formatDuration((hours * 3_600_000L).toLong())} left"
    }
    val current = abs(reading.currentMa ?: 0)
    val capacity = reading.capacityMah ?: 0
    if (current > 20 && capacity > 0) {
      val hours = (capacity * level / 100f) / current
      return "About ${formatDuration((hours * 3_600_000L).toLong())} left"
    }
    return "Measuring drain…"
  }

  /** A small white-on-transparent status-bar icon with the live percentage. */
  private fun statusIcon(
    style: Int,
    level: Int?,
  ): Icon {
    val size = 96
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
      }
    val text =
      when (style) {
        BatterySettings.ICON_RING -> level?.toString() ?: "—"
        BatterySettings.ICON_PERCENT -> level?.toString() ?: "—"
        else -> (level?.toString() ?: "—") + "°"
      }
    if (style == BatterySettings.ICON_RING) {
      paint.style = Paint.Style.STROKE
      paint.strokeWidth = 7f
      canvas.drawCircle(size / 2f, size / 2f, 39f, paint)
      paint.style = Paint.Style.FILL
      paint.textSize = 40f
    } else {
      paint.textSize = if (text.length <= 2) 74f else 61f
    }
    val baseline = size / 2f - (paint.ascent() + paint.descent()) / 2f
    canvas.drawText(text, size / 2f, baseline, paint)
    return Icon.createWithBitmap(bitmap)
  }

  private fun action(
    label: String,
    action: String,
    requestCode: Int,
  ): Notification.Action {
    val intent = Intent(context, BatteryMonitorService::class.java).setAction(action)
    val pending = PendingIntent.getService(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    return Notification.Action.Builder(Icon.createWithResource(context, R.drawable.ic_stat_battery), label, pending).build()
  }

  private fun openIntent(): PendingIntent =
    PendingIntent.getActivity(
      context,
      0,
      MainActivity.launchIntent(context, MainActivity.SCREEN_BATTERY),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

  private fun BatteryReading.chargerLabel(): String =
    when (plugged) {
      Plugged.AC -> "AC"
      Plugged.USB -> "USB"
      Plugged.WIRELESS -> "Wireless"
      Plugged.DOCK -> "Dock"
      Plugged.NONE -> "Battery"
    }

  fun ensureChannels() {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    if (manager.getNotificationChannel(CHANNEL_MONITOR) == null) {
      manager.createNotificationChannel(
        NotificationChannel(
          CHANNEL_MONITOR,
          context.getString(R.string.channel_monitor_name),
          NotificationManager.IMPORTANCE_LOW,
        ).apply {
          description = context.getString(R.string.channel_monitor_desc)
          setShowBadge(false)
          enableVibration(false)
          setSound(null, null)
          lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        },
      )
    }
    if (manager.getNotificationChannel(CHANNEL_ALARMS) == null) {
      manager.createNotificationChannel(
        NotificationChannel(
          CHANNEL_ALARMS,
          context.getString(R.string.channel_alarms_name),
          NotificationManager.IMPORTANCE_HIGH,
        ).apply {
          description = context.getString(R.string.channel_alarms_desc)
          enableVibration(true)
          setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)
        },
      )
    }
    if (manager.getNotificationChannel(CHANNEL_SUMMARY) == null) {
      manager.createNotificationChannel(
        NotificationChannel(
          CHANNEL_SUMMARY,
          context.getString(R.string.channel_summary_name),
          NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
          description = context.getString(R.string.channel_summary_desc)
          setShowBadge(false)
        },
      )
    }
  }

  companion object {
    private const val CHANNEL_MONITOR = "battery_monitor"
    private const val CHANNEL_ALARMS = "battery_alarms"
    private const val CHANNEL_SUMMARY = "battery_reports"
    private const val ALARM_BASE_ID = 500
    private const val SUMMARY_BASE_ID = 600
  }
}
