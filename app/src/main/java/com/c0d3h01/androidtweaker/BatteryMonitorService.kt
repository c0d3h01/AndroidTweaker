package com.c0d3h01.androidtweaker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.c0d3h01.androidtweaker.data.BatteryReader
import com.c0d3h01.androidtweaker.data.BatteryReading
import com.c0d3h01.androidtweaker.data.BatterySample
import com.c0d3h01.androidtweaker.data.BatterySession
import com.c0d3h01.androidtweaker.data.BatterySettings
import com.c0d3h01.androidtweaker.data.BatteryStatus
import com.c0d3h01.androidtweaker.data.BatteryStore
import com.c0d3h01.androidtweaker.data.FeatureEnforcer
import com.c0d3h01.androidtweaker.data.FeatureStore
import com.c0d3h01.androidtweaker.data.Plugged
import com.c0d3h01.androidtweaker.data.chargeQuality
import com.c0d3h01.androidtweaker.data.deepSleepNow
import com.c0d3h01.androidtweaker.data.drainRate
import com.c0d3h01.androidtweaker.data.estimateToFull
import com.c0d3h01.androidtweaker.data.formatCurrent
import com.c0d3h01.androidtweaker.data.formatDuration
import com.c0d3h01.androidtweaker.data.formatMah
import com.c0d3h01.androidtweaker.data.formatPower
import com.c0d3h01.androidtweaker.data.formatTemp
import com.c0d3h01.androidtweaker.data.formatVoltage
import com.c0d3h01.androidtweaker.data.monitorRefreshDelayMs
import com.c0d3h01.androidtweaker.data.qualityTier
import com.c0d3h01.androidtweaker.util.ShellAccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.WeekFields
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The live battery monitor: one foreground notification, live status-bar value,
 * session tracking, charging summaries and every alarm on the Battery screen.
 */
class BatteryMonitorService : Service() {
  enum class MonitorStatus { STOPPED, STARTING, RUNNING, UNAVAILABLE }

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val changes = Channel<Unit>(Channel.CONFLATED)
  private val receiver =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        changes.trySend(Unit)
      }
    }
  private var unregisterSettings: (() -> Unit)? = null
  private var receiverRegistered = false
  private var charge: SessionBuilder? = null
  private var discharge: SessionBuilder? = null
  private var lastReading: BatteryReading? = null
  private var lastSampleAt: Long? = null
  private var lastPublishedAt = 0L
  private var publishedReading: BatteryReading? = null
  private var publishedIconStyle: Int? = null
  private val lastAlarmAt = mutableMapOf<String, Long>()
  private var fullNotified = false
  private var lowNotified = false
  private var levelNotified: Int? = null
  private var lastAnomalyAt = 0L
  private var enforcerRetryAt = 0L

  override fun onCreate() {
    super.onCreate()
    ensureChannels()
  }

  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int,
  ): Int {
    if (intent?.action == ACTION_STOP) {
      BatteryStore.update(this) { it.copy(monitorEnabled = false) }
    } else if (!receiverRegistered && (intent?.action == ACTION_START || BatteryStore.settings(this).monitorEnabled)) {
      try {
        startForegroundCompat(
          monitorNotificationBuilder()
            .setSmallIcon(R.drawable.ic_stat_battery)
            .setContentTitle(getString(R.string.battery_monitor_starting))
            .build(),
        )
      } catch (failure: RuntimeException) {
        Log.w(TAG, "Unable to promote battery monitor", failure)
        monitorStatus.value = MonitorStatus.UNAVAILABLE
        stopSelf(startId)
        return START_NOT_STICKY
      }
    }
    if (!BatteryStore.settings(this).monitorEnabled) {
      if (receiverRegistered) {
        changes.trySend(Unit)
        return START_NOT_STICKY
      }
      monitorStatus.value = MonitorStatus.STOPPED
      stopForeground(STOP_FOREGROUND_REMOVE)
      stopSelf(startId)
      return START_NOT_STICKY
    }
    if (!receiverRegistered) {
      val filter =
        IntentFilter(Intent.ACTION_BATTERY_CHANGED).apply {
          addAction(Intent.ACTION_POWER_CONNECTED)
          addAction(Intent.ACTION_POWER_DISCONNECTED)
          addAction(Intent.ACTION_SCREEN_ON)
          addAction(Intent.ACTION_SCREEN_OFF)
        }
      try {
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        receiverRegistered = true
        unregisterSettings = BatteryStore.observeSettings(this) { changes.trySend(Unit) }
      } catch (failure: RuntimeException) {
        Log.w(TAG, "Unable to register battery monitor", failure)
        monitorStatus.value = MonitorStatus.UNAVAILABLE
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf(startId)
        return START_NOT_STICKY
      }
      monitorStatus.value = MonitorStatus.RUNNING
      changes.trySend(Unit)
      scope.launch { monitor() }
    }
    return START_STICKY
  }

  private suspend fun monitor() {
    var nextDelayMs: Long? = null
    var retryDelayMs = 0L
    while (scope.coroutineContext.isActive) {
      val timeout = nextDelayMs
      if (timeout == null) changes.receive() else withTimeoutOrNull(timeout) { changes.receive() }
      delay(EVENT_BATCH_MS)
      changes.tryReceive()
      try {
        val settings = BatteryStore.settings(this)
        if (!settings.monitorEnabled) {
          releaseCharging()
          withContext(Dispatchers.Main.immediate) {
            if (!BatteryStore.settings(this@BatteryMonitorService).monitorEnabled) {
              monitorStatus.value = MonitorStatus.STOPPED
              stopForeground(STOP_FOREGROUND_REMOVE)
              stopSelf()
            }
          }
          continue
        }
        val reading = BatteryReader.read(this)
        scope.coroutineContext.ensureActive()
        val previous = lastReading
        val edge = previous == null || previous.screenOn != reading.screenOn || previous.plugged != reading.plugged
        updateSessions(reading, settings)
        recordSample(reading, edge)
        evaluateAlarms(reading, settings)
        edgeReports(reading, settings)
        lastReading = reading
        val features = FeatureStore.load(this)
        val chargingControlConnected = reading.isPlugged || (features.chargingActive && FeatureEnforcer.ownsChargeHold(this))
        val chargeControl = features.chargingActive && chargingControlConnected && ShellAccess.runnerOrNull(requireRoot = true) != null
        nextDelayMs =
          monitorRefreshDelayMs(
            settings.refreshSeconds,
            reading.screenOn,
            chargingControlConnected,
            reading.isCharging,
            chargeControl,
            settings.notifyHighTemp || settings.notifyLowTemp || settings.overnightReport ||
              settings.weeklySummary || settings.anomalyAlert,
          )
        publishReading(reading, settings, nextDelayMs)
        val elapsed = SystemClock.elapsedRealtime()
        if (elapsed >= enforcerRetryAt) {
          try {
            FeatureEnforcer.tick(this, reading, previous, features)
          } catch (cancelled: CancellationException) {
            throw cancelled
          } catch (failure: Exception) {
            Log.w(TAG, "Optional battery feature failed", failure)
            enforcerRetryAt = elapsed + 60_000L
          }
        }
        retryDelayMs = 0L
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (failure: Exception) {
        if (retryDelayMs == 0L) Log.w(TAG, "Battery monitor update failed", failure)
        retryDelayMs = (retryDelayMs * 2).coerceIn(30_000L, 300_000L)
        delay(retryDelayMs)
        changes.trySend(Unit)
      }
    }
  }

  private suspend fun releaseCharging() {
    val restored = try {
      FeatureEnforcer.releaseCharging(this)
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (failure: Exception) {
      Log.w(TAG, "Unable to restore charging controls", failure)
      false
    }
    if (!restored) {
      alarm(
        "charge_restore_failed",
        getString(R.string.battery_charge_restore_failed),
        getString(R.string.battery_charge_restore_detail),
      )
    }
  }

  private suspend fun publishReading(reading: BatteryReading, settings: BatterySettings, nextDelayMs: Long?) {
    val now = SystemClock.elapsedRealtime()
    val previous = publishedReading
    if (previous != null && previous.level == reading.level && previous.status == reading.status &&
      previous.plugged == reading.plugged && previous.screenOn == reading.screenOn &&
      publishedIconStyle == settings.iconStyle && now - lastPublishedAt < (nextDelayMs ?: 60_000L)
    ) {
      return
    }
    val notification = notification(reading, settings)
    withContext(Dispatchers.Main.immediate) {
      if (monitorStatus.value == MonitorStatus.RUNNING) {
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification)
        BatteryWidgetProvider.push(this@BatteryMonitorService, reading)
      }
    }
    publishedReading = reading
    publishedIconStyle = settings.iconStyle
    lastPublishedAt = now
  }

  private fun recordSample(reading: BatteryReading, edge: Boolean) {
    val level = reading.level ?: return
    val now = SystemClock.elapsedRealtime()
    if (!edge && lastSampleAt?.let { now - it < 55_000L } == true) return
    lastSampleAt = now
    BatteryStore.addSample(
      this,
      BatterySample(
        at = reading.at,
        level = level,
        tempC = reading.tempC,
        currentMa = reading.currentMa,
        voltageMv = reading.voltageMv,
        charging = reading.isPlugged,
        screenOn = reading.screenOn,
      ),
    )
  }

  private fun updateSessions(
    reading: BatteryReading,
    settings: BatterySettings,
  ) {
    val level = reading.level ?: return
    val previous = lastReading
    when {
      reading.isCharging || (reading.isPlugged && reading.status == BatteryStatus.FULL) -> {
        if (charge == null) {
          charge = SessionBuilder(reading, charging = true)
          fullNotified = false
        } else {
          charge?.update(reading, chargingNow = reading.isCharging)
        }
        if (discharge != null) {
          discharge?.update(reading, chargingNow = reading.isCharging)
          discharge?.finish(level, charging = false)?.let { BatteryStore.addSession(this, it) }
          discharge = null
        }
      }

      reading.isPlugged -> {
        if (discharge != null) {
          discharge?.update(reading, chargingNow = false)
          discharge?.finish(level, charging = false)?.let { BatteryStore.addSession(this, it) }
          discharge = null
        }
        charge?.update(reading, chargingNow = false)
      }

      else -> {
        if (charge != null) {
          charge?.update(reading, chargingNow = false)
          val session = charge?.finish(level, charging = true)
          charge = null
          if (session != null) {
            BatteryStore.addSession(this, session)
            if (settings.chargingSummary && session.durationMs >= 2 * 60_000 &&
              session.endLevel - session.startLevel >= 2
            ) {
              postChargingSummary(session)
            }
          }
        }
        if (discharge == null) {
          discharge = SessionBuilder(reading, charging = false)
        } else {
          discharge?.update(reading, chargingNow = reading.isCharging)
        }
      }
    }
    if (previous?.level != level) {
      if (level > (previous?.level ?: level)) lowNotified = false
      if (level < 100) fullNotified = false
      if (levelNotified != null && abs(level - levelNotified!!) >= 3) levelNotified = null
    }
  }

  private fun evaluateAlarms(
    reading: BatteryReading,
    settings: BatterySettings,
  ) {
    val level = reading.level ?: return
    val previous = lastReading
    val now = System.currentTimeMillis()
    if (settings.notifyFull && !fullNotified &&
      (reading.status == BatteryStatus.FULL || (level >= 100 && reading.isPlugged))
    ) {
      fullNotified = true
      alarm("full", "Battery full", "$level% · unplug when convenient")
    }
    if (settings.notifyLow && !lowNotified && !reading.isPlugged && level <= settings.lowLevel) {
      lowNotified = true
      alarm("low", "Battery low", "$level% left · plug in soon")
    }
    if (settings.notifyLevel && levelNotified == null) {
      val crossedUp = reading.isPlugged && previous?.level != null && previous.level!! < settings.levelAlarm && level >= settings.levelAlarm
      val crossedDown =
        !reading.isPlugged && previous?.level != null && previous.level!! > settings.levelAlarm && level <= settings.levelAlarm
      if (crossedUp || crossedDown) {
        levelNotified = level
        alarm(
          "level",
          if (crossedUp) "Battery reached ${settings.levelAlarm}%" else "Battery dropped to ${settings.levelAlarm}%",
          if (crossedUp) "You can unplug now" else "$level% remaining",
        )
      }
    }
    val temp = reading.tempC
    if (settings.notifyHighTemp && temp != null && temp >= settings.highTempC) {
      if (now - (lastAlarmAt["highTemp"] ?: 0) >= 30 * 60_000L) {
        lastAlarmAt["highTemp"] = now
        alarm("highTemp", "Battery is hot", formatTemp(temp) + " · above ${settings.highTempC}°C")
      }
    }
    if (settings.notifyLowTemp && temp != null && temp <= settings.lowTempC) {
      if (now - (lastAlarmAt["lowTemp"] ?: 0) >= 30 * 60_000L) {
        lastAlarmAt["lowTemp"] = now
        alarm("lowTemp", "Battery is cold", formatTemp(temp) + " · below ${settings.lowTempC}°C")
      }
    }
  }

  private fun edgeReports(
    reading: BatteryReading,
    settings: BatterySettings,
  ) {
    val now = LocalDateTime.now()
    if (settings.overnightReport && now.hour in 6..10) {
      val today = LocalDate.now().toEpochDay()
      if (BatteryStore.lastOvernightDay(this) != today) {
        BatteryStore.markOvernightDay(this, today)
        overnightReport()
      }
    }
    if (settings.weeklySummary && now.dayOfWeek.value == 1 && now.hour >= 8) {
      val week =
        LocalDate.now().get(WeekFields.ISO.weekBasedYear()).toString() + "-" +
          LocalDate.now().get(WeekFields.ISO.weekOfWeekBasedYear())
      if (BatteryStore.lastWeeklyWeek(this) != week) {
        BatteryStore.markWeeklyWeek(this, week)
        weeklyReport()
      }
    }
    if (settings.anomalyAlert && !reading.isPlugged && !reading.screenOn) {
      val nowMs = System.currentTimeMillis()
      if (nowMs - lastAnomalyAt >= 6 * 3_600_000L) {
        val rate = drainRate(BatteryStore.samples(this), false, nowMs) ?: 0f
        if (rate >= 8f) {
          lastAnomalyAt = nowMs
          alarm("anomaly", "Unusual battery drain", "%.1f%%/h with the screen off".format(rate))
        }
      }
    }
  }

  private fun overnightReport() {
    val samples = BatteryStore.samples(this)
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
    summary(
      "overnight",
      "Overnight report",
      "Battery dropped $drop% · %.1f%%/h".format(rate),
      "From ${first.level}% to ${last.level}% over ${formatDuration(
        last.at - first.at,
      )}. Deep sleep and screen-off drain are on the Battery screen.",
    )
  }

  private fun weeklyReport() {
    val week = BatteryStore.summarySince(this, System.currentTimeMillis() - 7 * 24 * 3_600_000L)
    if (week.chargeCount == 0 && week.drainPercent == 0) return
    summary(
      "weekly",
      "Weekly battery report",
      "${week.drainPercent}% drained · ${week.chargeCount} charges",
      "Added ${formatMah(week.addedMah)} · screen on ${formatDuration(week.screenOnMs)} · deep sleep ${formatDuration(week.deepSleepMs)}" +
        (week.averageQuality?.let { " · charge quality $it" } ?: ""),
    )
  }

  private fun postChargingSummary(session: BatterySession) {
    val settings = BatteryStore.settings(this)
    if (!settings.chargingSummary) return
    val delta = (session.endLevel - session.startLevel).coerceAtLeast(0)
    val rate = if (session.durationMs > 0) delta / (session.durationMs / 3_600_000f) else 0f
    val title = "⚡ Charged ${session.startLevel}% → ${session.endLevel}% in ${formatDuration(session.durationMs)}"
    val body =
      "+$delta% · ${formatMah(session.addedMah)} · ${"%.0f".format(rate)}%/h\n" +
        "${formatPower(session.peakPowerW)} peak power · ${formatTemp(session.peakTempC)} peak temp" +
        (if (session.quality > 0) " · quality ${session.quality} (${qualityTier(session.quality)})" else "")
    summary("charging-summary", title, "Charging summary", body)
  }

  private fun alarm(
    key: String,
    title: String,
    body: String,
  ) {
    val manager = getSystemService(NotificationManager::class.java) ?: return
    val open = openIntent()
    val notification =
      Notification
        .Builder(this, CHANNEL_ALARMS)
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

  private fun summary(
    key: String,
    title: String,
    body: String,
    big: String,
  ) {
    val manager = getSystemService(NotificationManager::class.java) ?: return
    val notification =
      Notification
        .Builder(this, CHANNEL_SUMMARY)
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

  private fun notification(
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

  private fun monitorNotificationBuilder(): Notification.Builder =
    Notification
      .Builder(this, CHANNEL_MONITOR)
      .setContentIntent(openIntent())
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setShowWhen(false)
      .setCategory(Notification.CATEGORY_SERVICE)
      .setVisibility(Notification.VISIBILITY_PUBLIC)
      .addAction(action(getString(R.string.battery_monitor_pause), ACTION_STOP, 1))
      .apply {
        if (Build.VERSION.SDK_INT >= 31) {
          setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
      }

  private fun estimateRemainingText(reading: BatteryReading): String {
    val level = reading.level ?: return "Reading battery…"
    val rate = drainRate(BatteryStore.samples(this), reading.screenOn, System.currentTimeMillis())
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
    val intent = Intent(this, BatteryMonitorService::class.java).setAction(action)
    val pending = PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    return Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_stat_battery), label, pending).build()
  }

  private fun openIntent(): PendingIntent =
    PendingIntent.getActivity(
      this,
      0,
      MainActivity.launchIntent(this, MainActivity.SCREEN_BATTERY),
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

  private fun ensureChannels() {
    val manager = getSystemService(NotificationManager::class.java) ?: return
    if (manager.getNotificationChannel(CHANNEL_MONITOR) == null) {
      manager.createNotificationChannel(
        NotificationChannel(
          CHANNEL_MONITOR,
          getString(R.string.channel_monitor_name),
          NotificationManager.IMPORTANCE_LOW,
        ).apply {
          description = getString(R.string.channel_monitor_desc)
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
          getString(R.string.channel_alarms_name),
          NotificationManager.IMPORTANCE_HIGH,
        ).apply {
          description = getString(R.string.channel_alarms_desc)
          enableVibration(true)
          setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)
        },
      )
    }
    if (manager.getNotificationChannel(CHANNEL_SUMMARY) == null) {
      manager.createNotificationChannel(
        NotificationChannel(
          CHANNEL_SUMMARY,
          getString(R.string.channel_summary_name),
          NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
          description = getString(R.string.channel_summary_desc)
          setShowBadge(false)
        },
      )
    }
  }

  private fun startForegroundCompat(notification: Notification) {
    if (Build.VERSION.SDK_INT >= 34) {
      startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
  }

  override fun onDestroy() {
    scope.cancel()
    if (receiverRegistered) unregisterReceiver(receiver)
    unregisterSettings?.invoke()
    monitorStatus.compareAndSet(MonitorStatus.RUNNING, MonitorStatus.STOPPED)
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  /** Accumulates a charge or discharge session across monitor ticks. */
  private class SessionBuilder(
    private val reading: BatteryReading,
    private val charging: Boolean,
  ) {
    private val start = reading.at
    private val startLevel = reading.level ?: 0
    private var peakPowerW = reading.powerW ?: 0f
    private var peakTempC: Float? = reading.tempC
    private var addedMah = 0f
    private val startCounter = reading.chargeCounterUah
    private var lastCounter = reading.chargeCounterUah
    private var lastTick = SystemClock.elapsedRealtime()
    private var lastDeep = deepSleepNow()
    private var screenOnMs = 0L
    private var screenOffMs = 0L
    private var deepSleepMs = 0L
    private var lastCurrentMa = reading.currentMa
    private var lastScreenOn = reading.screenOn

    fun update(
      next: BatteryReading,
      chargingNow: Boolean,
    ) {
      val now = SystemClock.elapsedRealtime()
      val dt = (now - lastTick).coerceAtLeast(0)
      val powerDt = dt.coerceAtMost(120_000)
      lastTick = now
      val deep = deepSleepNow()
      deepSleepMs += (deep - lastDeep).coerceAtLeast(0)
      lastDeep = deep
      if (lastScreenOn) screenOnMs += dt else screenOffMs += dt
      lastScreenOn = next.screenOn
      if (charging && chargingNow) {
        val ma = abs(next.currentMa ?: lastCurrentMa ?: 0)
        if (ma > 0) addedMah += ma * powerDt / 3_600_000f
      }
      lastCounter = next.chargeCounterUah
      lastCurrentMa = next.currentMa
      peakPowerW = maxOf(peakPowerW, next.powerW ?: 0f)
      if (next.tempC != null) peakTempC = maxOf(peakTempC ?: next.tempC, next.tempC)
    }

    fun finish(
      endLevel: Int,
      charging: Boolean,
    ): BatterySession? {
      val end = System.currentTimeMillis()
      if (end - start < 45_000) return null
      val delta = (endLevel - startLevel).coerceAtLeast(0)
      val counterMah =
        if (startCounter != null && lastCounter != null) {
          (lastCounter!! - startCounter) / 1000f
        } else {
          0f
        }
      val added = counterMah.takeIf { it > 0f } ?: addedMah
      val capacity = if (charging && delta >= 15 && added > 0) (added / delta * 100f).roundToInt() else null
      return BatterySession(
        start = start,
        end = end,
        charging = charging,
        startLevel = startLevel,
        endLevel = endLevel,
        addedMah = added.roundToInt().coerceAtLeast(0),
        peakPowerW = peakPowerW,
        peakTempC = peakTempC,
        charger = reading.plugged,
        quality =
          if (charging) chargeQuality(startLevel, endLevel, peakTempC, peakPowerW) else 0,
        capacityMah = capacity?.takeIf { it in 500..20_000 },
        screenOnMs = screenOnMs,
        screenOffMs = screenOffMs,
        deepSleepMs = deepSleepMs,
      )
    }
  }

  companion object {
    const val ACTION_STOP = "com.c0d3h01.androidtweaker.BATTERY_MONITOR_STOP"
    private const val ACTION_START = "com.c0d3h01.androidtweaker.BATTERY_MONITOR_START"
    private const val TAG = "BatteryMonitor"
    private const val EVENT_BATCH_MS = 350L
    private const val CHANNEL_MONITOR = "battery_monitor"
    private const val CHANNEL_ALARMS = "battery_alarms"
    private const val CHANNEL_SUMMARY = "battery_reports"
    private const val NOTIFICATION_ID = 43
    private const val ALARM_BASE_ID = 500
    private const val SUMMARY_BASE_ID = 600
    private val monitorStatus = MutableStateFlow(MonitorStatus.STOPPED)
    val status = monitorStatus.asStateFlow()

    fun start(context: Context) {
      if (!BatteryStore.settings(context).monitorEnabled) return
      val current = monitorStatus.value
      if (current == MonitorStatus.RUNNING || current == MonitorStatus.STARTING ||
        !monitorStatus.compareAndSet(current, MonitorStatus.STARTING)
      ) {
        return
      }
      val intent = Intent(context, BatteryMonitorService::class.java).setAction(ACTION_START)
      try {
        if (context.startForegroundService(intent) == null) monitorStatus.value = MonitorStatus.UNAVAILABLE
      } catch (failure: RuntimeException) {
        Log.w(TAG, "Unable to start battery monitor", failure)
        monitorStatus.value = MonitorStatus.UNAVAILABLE
      }
    }

    fun stop(context: Context) {
      BatteryStore.update(context) { it.copy(monitorEnabled = false) }
      // The service observes the saved pause and releases its charging hold before stopping.
    }

    fun running(context: Context): Boolean = monitorStatus.value == MonitorStatus.RUNNING && BatteryStore.settings(context).monitorEnabled
  }
}
