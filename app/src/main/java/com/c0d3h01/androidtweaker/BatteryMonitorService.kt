package com.c0d3h01.androidtweaker

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.c0d3h01.androidtweaker.access.AccessManager
import com.c0d3h01.androidtweaker.battery.BatteryReader
import com.c0d3h01.androidtweaker.battery.BatteryReading
import com.c0d3h01.androidtweaker.battery.BatteryRepository
import com.c0d3h01.androidtweaker.battery.BatterySample
import com.c0d3h01.androidtweaker.battery.BatterySettings
import com.c0d3h01.androidtweaker.battery.deepSleepNow
import com.c0d3h01.androidtweaker.battery.monitor.BatteryAlertPolicy
import com.c0d3h01.androidtweaker.battery.monitor.BatteryNotifications
import com.c0d3h01.androidtweaker.battery.monitor.BatteryReportPolicy
import com.c0d3h01.androidtweaker.battery.monitor.SessionTracker
import com.c0d3h01.androidtweaker.battery.monitor.monitorRefreshDelayMs
import com.c0d3h01.androidtweaker.charging.ChargingController
import com.c0d3h01.androidtweaker.charging.ScreenOffAutomation
import com.c0d3h01.androidtweaker.tuning.TuningStore
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
  private val notifications by lazy { BatteryNotifications(this) }
  private val reports by lazy { BatteryReportPolicy(this, notifications) }
  private val sessions = SessionTracker()
  private val alerts = BatteryAlertPolicy()
  private var lastReading: BatteryReading? = null
  private var lastSampleAt: Long? = null
  private var lastPublishedAt = 0L
  private var publishedReading: BatteryReading? = null
  private var publishedIconStyle: Int? = null
  private var enforcerRetryAt = 0L

  override fun onCreate() {
    super.onCreate()
    notifications.ensureChannels()
  }

  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int,
  ): Int {
    if (intent?.action == ACTION_STOP) {
      BatteryRepository.update(this) { it.copy(monitorEnabled = false) }
    } else if (!receiverRegistered && (intent?.action == ACTION_START || BatteryRepository.settings(this).monitorEnabled)) {
      try {
        startForegroundCompat(
          notifications.monitorNotificationBuilder()
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
    if (!BatteryRepository.settings(this).monitorEnabled) {
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
        unregisterSettings = BatteryRepository.observeSettings(this) { changes.trySend(Unit) }
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
        val settings = BatteryRepository.settings(this)
        if (!settings.monitorEnabled) {
          releaseCharging()
          withContext(Dispatchers.Main.immediate) {
            if (!BatteryRepository.settings(this@BatteryMonitorService).monitorEnabled) {
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
        reports.evaluate(reading, settings)
        lastReading = reading
        val features = TuningStore.load(this)
        val chargingControlConnected = reading.isPlugged || (features.charging.active && ChargingController.ownsChargeHold(this))
        val chargeControl = features.charging.active && chargingControlConnected && AccessManager.runnerOrNull(requireRoot = true) != null
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
            ChargingController.tick(this, reading, previous, features)
            ScreenOffAutomation.tick(this, reading, previous, features)
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
      ChargingController.releaseCharging(this)
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (failure: Exception) {
      Log.w(TAG, "Unable to restore charging controls", failure)
      false
    }
    if (!restored) {
      notifications.alarm(
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
    val notification = notifications.notification(reading, settings)
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
    BatteryRepository.addSample(
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

  private fun updateSessions(reading: BatteryReading, settings: BatterySettings) {
    val completed = sessions.onReading(reading, SystemClock.elapsedRealtime(), deepSleepNow(), System.currentTimeMillis())
    if (sessions.startedCharge) alerts.onChargeStarted()
    completed.forEach { session ->
      BatteryRepository.addSession(this, session)
      if (session.charging && settings.chargingSummary && session.durationMs >= 2 * 60_000 &&
        session.endLevel - session.startLevel >= 2
      ) reports.chargingSummary(session)
    }
    alerts.onLevelChange(reading, lastReading)
  }

  private fun evaluateAlarms(reading: BatteryReading, settings: BatterySettings) {
    alerts.evaluate(reading, lastReading, settings, System.currentTimeMillis()).forEach {
      notifications.alarm(it.key, it.title, it.body)
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

  companion object {
    const val ACTION_STOP = "com.c0d3h01.androidtweaker.BATTERY_MONITOR_STOP"
    private const val ACTION_START = "com.c0d3h01.androidtweaker.BATTERY_MONITOR_START"
    private const val TAG = "BatteryMonitor"
    private const val EVENT_BATCH_MS = 350L
    private const val NOTIFICATION_ID = 43
    private val monitorStatus = MutableStateFlow(MonitorStatus.STOPPED)
    val status = monitorStatus.asStateFlow()

    fun start(context: Context) {
      if (!BatteryRepository.settings(context).monitorEnabled) return
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
      BatteryRepository.update(context) { it.copy(monitorEnabled = false) }
      // The service observes the saved pause and releases its charging hold before stopping.
    }

    fun running(context: Context): Boolean = monitorStatus.value == MonitorStatus.RUNNING && BatteryRepository.settings(context).monitorEnabled
  }
}
