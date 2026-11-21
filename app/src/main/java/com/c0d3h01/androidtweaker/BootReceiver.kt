package com.c0d3h01.androidtweaker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.c0d3h01.androidtweaker.profiles.ProfilePreferences

/** Reapplies the last selected profile after a reboot; no module required. */
class BootReceiver : BroadcastReceiver() {
  override fun onReceive(
    context: Context,
    intent: Intent,
  ) {
    if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
    BatteryMonitorService.start(context)
    val code = ProfilePreferences.currentCode(context)
    if (!ProfilePreferences.canReapply(context, code)) return

    val service =
      Intent(context, TweakApplyService::class.java)
        .setAction(TweakApplyService.ACTION_BOOT)
        .putExtra(TweakApplyService.EXTRA_CODE, code)
    runCatching {
      context.startForegroundService(service)
    }
  }
}
