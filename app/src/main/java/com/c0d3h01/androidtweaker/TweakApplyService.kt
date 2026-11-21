package com.c0d3h01.androidtweaker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.c0d3h01.androidtweaker.access.RootConnection
import com.c0d3h01.androidtweaker.engine.EngineFiles
import com.c0d3h01.androidtweaker.engine.TweakEngine
import com.c0d3h01.androidtweaker.profiles.Profile
import com.c0d3h01.androidtweaker.profiles.ProfilePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Short-lived foreground service used for boot reapply. Applying sysfs tweaks
 * from a manifest receiver would be killed as soon as onReceive returned.
 */
class TweakApplyService : Service() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var applying = false

  override fun onCreate() {
    super.onCreate()
    val manager = getSystemService(NotificationManager::class.java)
    if (manager.getNotificationChannel(CHANNEL_ID) == null) {
      manager.createNotificationChannel(
        NotificationChannel(CHANNEL_ID, "Boot tweak apply", NotificationManager.IMPORTANCE_LOW).apply {
          description = "Reapplies the selected AndroidTweaker profile after reboot"
          setShowBadge(false)
        },
      )
    }
  }

  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int,
  ): Int {
    if (applying) return START_NOT_STICKY
    val profile = Profile.fromCode(intent?.getIntExtra(EXTRA_CODE, -1) ?: -1)
    if (intent?.action != ACTION_BOOT || profile == null || !canReapply(profile)) {
      stopSelf(startId)
      return START_NOT_STICKY
    }
    startForegroundCompat(notification("Applying ${profile.label}…", ongoing = true))
    applying = true
    scope.launch {
      val result =
        runCatching {
          val runner = RootConnection.request()
          check(canReapply(profile)) { "Boot reapply disabled or profile changed" }
          TweakEngine.apply(this@TweakApplyService, runner, profile, fromBoot = true)
        }.getOrElse { e ->
          TweakEngine.Result(
            ok = false,
            exitCode = -1,
            output = e.message ?: "Boot apply failed",
            logPath = EngineFiles.logFile(this@TweakApplyService).absolutePath,
          )
        }
      EngineFiles.record(
        this@TweakApplyService,
        if (result.ok) "boot: reapplied ${profile.label}" else "boot: failed ${profile.label} (${result.output.trim().take(300)})",
      )
      notify(notification(if (result.ok) "${profile.label} reapplied" else "Boot apply failed", ongoing = false))
      delay(2_000)
      stopForeground(STOP_FOREGROUND_REMOVE)
      stopSelf()
    }
    return START_NOT_STICKY
  }

  private fun canReapply(profile: Profile): Boolean =
    ProfilePreferences.canReapply(this, profile.code)

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }

  private fun startForegroundCompat(notification: Notification) {
    if (Build.VERSION.SDK_INT >= 34) {
      startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
  }

  private fun notify(notification: Notification) {
    getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
  }

  private fun notification(
    text: String,
    ongoing: Boolean,
  ): Notification {
    val open =
      PendingIntent.getActivity(
        this,
        0,
        MainActivity.launchIntent(this, MainActivity.SCREEN_TWEAKS),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    return Notification
      .Builder(this, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_stat_tweak)
      .setContentTitle("AndroidTweaker")
      .setContentText(text)
      .setContentIntent(open)
      .setOngoing(ongoing)
      .setCategory(Notification.CATEGORY_SERVICE)
      .build()
  }

  private val Profile.label: String
    get() = name.lowercase().replaceFirstChar { it.uppercase() }

  companion object {
    const val ACTION_BOOT = "com.c0d3h01.androidtweaker.BOOT_APPLY"
    const val EXTRA_CODE = "profile_code"
    private const val CHANNEL_ID = "boot_apply"
    private const val NOTIFICATION_ID = 41
  }
}
