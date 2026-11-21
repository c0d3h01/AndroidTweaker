package com.c0d3h01.androidtweaker.settings

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.c0d3h01.androidtweaker.MainActivity
import com.c0d3h01.androidtweaker.R
import com.c0d3h01.androidtweaker.common.clock
import com.c0d3h01.androidtweaker.profiles.Profile

/**
 * Low-priority, single-slot notifications for meaningful app actions.
 *
 * Only confirmed operations that the user may want to know about after leaving
 * the app post here (profile applied, log/diagnostics shared). Minor actions
 * such as copying a line stay as toasts. Every post replaces the previous one
 * and the channel never makes a sound, so activity cannot pile up.
 */
object ActivityNotifications {
  private const val CHANNEL_ID = "activity_updates"
  private const val NOTIFICATION_ID = 42
  private const val PREFS = "settings"
  private const val KEY_ENABLED = "notify_activity"

  fun enabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, true)

  fun setEnabled(
    context: Context,
    enabled: Boolean,
  ) {
    prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
  }

  fun systemAllowed(context: Context): Boolean =
    context.getSystemService(NotificationManager::class.java)?.areNotificationsEnabled() ?: false

  /** System permission plus the activity channel itself; a blocked channel silently drops posts. */
  fun channelAllowed(context: Context): Boolean {
    if (!systemAllowed(context)) return false
    val channel = context.getSystemService(NotificationManager::class.java)?.getNotificationChannel(CHANNEL_ID)
    return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
  }

  /** Opens this app's activity channel, creating it first so the deep link has a destination. */
  fun openSettings(context: Context): Boolean {
    context.getSystemService(NotificationManager::class.java)?.let { ensureChannel(context, it) }
    val intent =
      Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID)
    return runCatching { context.startActivity(intent) }.isSuccess
  }

  fun profileApplied(
    context: Context,
    profile: Profile,
  ) = post(context, "Profile applied", "${profile.label} is active · ${clock(context)}", MainActivity.SCREEN_TWEAKS)

  fun logExported(
    context: Context,
    source: LogSource,
    lines: Int,
  ) = post(context, "Log ready to share", "${source.title} · $lines lines · ${clock(context)}", MainActivity.SCREEN_LOGS)

  fun diagnosticsShared(context: Context) =
    post(context, "Diagnostics ready to share", "Device, root and engine summary · ${clock(context)}", MainActivity.SCREEN_TOOLS)

  private fun post(
    context: Context,
    title: String,
    body: String,
    screen: String,
  ) {
    if (!enabled(context) || !channelAllowed(context)) return
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    ensureChannel(context, manager)
    val open =
      PendingIntent.getActivity(
        context,
        0,
        MainActivity.launchIntent(context, screen),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    val notification =
      Notification
        .Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_tweak)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(Notification.BigTextStyle().bigText(body))
        .setContentIntent(open)
        .setAutoCancel(true)
        .setOnlyAlertOnce(true)
        .setCategory(Notification.CATEGORY_STATUS)
        .build()
    manager.notify(NOTIFICATION_ID, notification)
  }

  private fun ensureChannel(
    context: Context,
    manager: NotificationManager,
  ) {
    if (manager.getNotificationChannel(CHANNEL_ID) != null) return
    manager.createNotificationChannel(
      NotificationChannel(
        CHANNEL_ID,
        context.getString(R.string.channel_activity_name),
        NotificationManager.IMPORTANCE_LOW,
      ).apply {
        description = context.getString(R.string.channel_activity_desc)
        setShowBadge(false)
        enableVibration(false)
        setSound(null, null)
      },
    )
  }

  private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

  private val Profile.label: String
    get() = name.lowercase().replaceFirstChar { it.uppercase() }
}
