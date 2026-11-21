package com.c0d3h01.androidtweaker.profiles

import android.content.Context
import com.c0d3h01.androidtweaker.access.AccessManager

object ProfilePreferences {
  fun currentCode(context: Context): Int =
    Profile.fromCode(prefs(context).getString("profile", null)?.toIntOrNull() ?: -1)?.code ?: -1

  fun lastProfileAccess(context: Context): AccessManager.Mode? =
    AccessManager.Mode.entries.firstOrNull { it.name == prefs(context).getString("profile_access", null) }

  fun reapplyOnBoot(context: Context): Boolean = prefs(context).getBoolean("reapply_on_boot", false)

  fun canReapply(context: Context, code: Int): Boolean =
    code != -1 && reapplyOnBoot(context) &&
      lastProfileAccess(context) == AccessManager.Mode.ROOT && currentCode(context) == code

  fun setReapplyOnBoot(context: Context, enabled: Boolean) {
    prefs(context).edit().putBoolean("reapply_on_boot", enabled).apply()
  }

  fun confirm(context: Context, code: Int, mode: AccessManager.Mode) {
    prefs(context).edit()
      .putString("profile", code.toString())
      .putString("profile_access", mode.name)
      .apply()
  }

  private fun prefs(context: Context) = context.getSharedPreferences("profile", Context.MODE_PRIVATE)
}
