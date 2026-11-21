package com.c0d3h01.androidtweaker.profiles

import android.content.Context
import com.c0d3h01.androidtweaker.engine.EngineFiles
import java.io.File
import java.util.Locale

/** Persists [ProfileSettings] per profile and writes the conf the engine sources. */
object ProfileSettingsStore {
  private const val PREFS = "tweak_groups"

  fun load(
    context: Context,
    profile: Profile,
  ): ProfileSettings {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val prefix = "${profile.name.lowercase(Locale.US)}."
    // A profile saved before this feature existed has no keys: keep every group.
    val stored = ProfileGroup.entries.filter { prefs.contains(prefix + it.key) }
    val groups =
      if (stored.isEmpty()) {
        ProfileGroup.entries.toSet()
      } else {
        stored.filter { prefs.getBoolean(prefix + it.key, true) }.toSet()
      }

    fun int(key: String): Int? = if (prefs.contains(prefix + key)) prefs.getInt(prefix + key, 0) else null
    return ProfileSettings(
      groups = groups,
      cpuCeiling = int("cpuCeiling"),
      gpuCeiling = int("gpuCeiling"),
      swappiness = int("swappiness"),
      readAheadKb = int("readAhead"),
    ).sanitized()
  }

  fun save(
    context: Context,
    profile: Profile,
    prefs: ProfileSettings,
  ) {
    val clean = prefs.sanitized()
    val prefix = "${profile.name.lowercase(Locale.US)}."
    val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
    ProfileGroup.entries.forEach { editor.putBoolean(prefix + it.key, it in clean.groups) }
    listOf(
      "cpuCeiling" to clean.cpuCeiling,
      "gpuCeiling" to clean.gpuCeiling,
      "swappiness" to clean.swappiness,
      "readAhead" to clean.readAheadKb,
    ).forEach { (key, value) ->
      if (value == null) editor.remove(prefix + key) else editor.putInt(prefix + key, value)
    }
    editor.apply()
  }

  fun confFile(
    context: Context,
    profile: Profile,
  ): File =
    File(
      EngineFiles.directory(context),
      "custom/${profile.name.lowercase(Locale.US)}.conf",
    )

  /** Must run before every apply, including the boot reapply. */
  fun writeConf(
    context: Context,
    profile: Profile,
    prefs: ProfileSettings = load(context, profile),
  ): File? {
    val file = confFile(context, profile)
    return runCatching {
      file.parentFile?.mkdirs()
      file.writeText(prefs.conf())
      file.setReadable(true, false)
      file
    }.getOrNull()
  }
}
