package com.c0d3h01.androidtweaker.profiles

/**
 * Per-profile overrides. Every field is nullable-on-purpose: null means "keep
 * what the profile ships", so a fresh install applies exactly the built-in set.
 */
data class ProfileSettings(
  val groups: Set<ProfileGroup> = ProfileGroup.entries.toSet(),
  val cpuCeiling: Int? = null,
  val gpuCeiling: Int? = null,
  val swappiness: Int? = null,
  val readAheadKb: Int? = null,
) {
  val enabledCount: Int
    get() = groups.size

  /** Recommended leaves thermal tuning alone; every other group remains selected. */
  fun withPreset(preset: ProfilePreset): ProfileSettings =
    when (preset) {
      ProfilePreset.ALL -> copy(groups = ProfileGroup.entries.toSet())
      ProfilePreset.RECOMMENDED -> copy(groups = ProfileGroup.entries.toSet() - ProfileGroup.THERMAL)
      ProfilePreset.NONE -> copy(groups = emptySet())
    }

  /** Clamp anything a stored preference or a migration could have pushed out of range. */
  fun sanitized(): ProfileSettings =
    copy(
      cpuCeiling = cpuCeiling?.coerceIn(CEILING_RANGE),
      gpuCeiling = gpuCeiling?.coerceIn(CEILING_RANGE),
      swappiness = swappiness?.coerceIn(SWAPPINESS_RANGE),
      readAheadKb = readAheadKb?.coerceIn(READ_AHEAD_RANGE),
    )

  /**
   * Shell fragment sourced by the profile script after its own values, so a
   * ceiling is a percentage of the device maximum rather than of the profile.
   */
  fun conf(): String {
    val clean = sanitized()
    val groups = ProfileGroup.entries.filter { it in clean.groups }.joinToString(" ") { it.key }
    return buildString {
      appendLine("# AndroidTweaker — generated per-profile overrides. Rewritten on every apply.")
      appendLine("TWEAK_GROUPS=\"$groups\"")
      clean.cpuCeiling?.let { appendLine("[ -n \"\${CPU_MAXFREQ:-}\" ] && CPU_MAX_FREQ=\$((CPU_MAXFREQ * $it / 100))") }
      clean.gpuCeiling?.let { appendLine("[ -n \"\${GPU_MAXFREQ:-}\" ] && GPU_MAX_FREQ_SET=\$((GPU_MAXFREQ * $it / 100))") }
      clean.swappiness?.let { appendLine("VM_SWAPPINESS=$it") }
      clean.readAheadKb?.let { appendLine("IO_READ_AHEAD=$it") }
    }
  }

  companion object {
    val CEILING_RANGE = 20..100
    val SWAPPINESS_RANGE = 0..200
    val READ_AHEAD_RANGE = 32..4096
  }
}
