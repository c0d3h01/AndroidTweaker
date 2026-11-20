package com.c0d3h01.androidtweaker.data

import android.content.Context
import java.io.File
import java.util.Locale

/**
 * Feature sections, mirroring the grouping the engine exposes in the UI:
 * core kernel work, memory, power, network, display, system rules.
 */
enum class TweakSection(
  val label: String,
  val blurb: String,
) {
  CORE("Core tweaks", "CPU, GPU, kernel and scheduler"),
  MEMORY("Memory & storage", "VM, filesystem and page merging"),
  POWER("Thermal & doze", "Heat limits and idle timings"),
  NETWORK("Network", "TCP stack and socket buffers"),
  DISPLAY("Display", "Touch boost, wake and power hints"),
  SYSTEM("System rules", "Process priorities and system services"),
}

/**
 * One apply_* family in the bundled engine. [key] is what `want()` in
 * lib/common.sh matches, so the two lists must stay in step.
 */
enum class TweakGroup(
  val key: String,
  val label: String,
  val blurb: String,
  val section: TweakSection,
) {
  CPU("cpu", "CPU", "Governor, clock limits, boost and input timing", TweakSection.CORE),
  GPU("gpu", "GPU", "Max clock, power level and idle timer", TweakSection.CORE),
  KERNEL("kernel", "Kernel & boost", "cpufreq flags, cgroup shares, entropy, wakelocks", TweakSection.CORE),
  SCHED("sched", "Scheduler", "Kernel scheduler features, latencies and tunables", TweakSection.CORE),
  STUNE("stune", "CPU boost", "Top-app and foreground boost values", TweakSection.CORE),
  IO("io", "Storage I/O", "Queue depth, read-ahead and merge policy", TweakSection.CORE),
  VM("vm", "Memory", "Swappiness, dirty ratios and writeback", TweakSection.MEMORY),
  FS("fs", "Filesystem", "Inotify and file-lease policy", TweakSection.MEMORY),
  KSM("ksm", "KSM", "Kernel same-page merging", TweakSection.MEMORY),
  THERMAL("thermal", "Thermal", "msm_thermal limits and power-efficient workqueues", TweakSection.POWER),
  DOZE("doze", "Doze timings", "Aggressive idle constants on battery profiles", TweakSection.POWER),
  NETWORK("network", "Network stack", "TCP timeouts and socket buffer sizes", TweakSection.NETWORK),
  DISPLAY("display", "Display", "Double-tap to wake, touch boost and power hints", TweakSection.DISPLAY),
  RENICE("renice", "Priorities", "Nice values for system processes", TweakSection.SYSTEM),
  SYSTEM("system", "Doze & system", "Doze constants, logd, perfd and mpdecision", TweakSection.SYSTEM),
}

/** Preset choices offered above the group list. */
enum class TweakPreset(
  val label: String,
) {
  ALL("All"),
  RECOMMENDED("Recommended"),
  NONE("None"),
}

/**
 * Per-profile overrides. Every field is nullable-on-purpose: null means "keep
 * what the profile ships", so a fresh install applies exactly the built-in set.
 */
data class TweakPrefs(
  val groups: Set<TweakGroup> = TweakGroup.entries.toSet(),
  val cpuCeiling: Int? = null,
  val gpuCeiling: Int? = null,
  val swappiness: Int? = null,
  val readAheadKb: Int? = null,
) {
  val enabledCount: Int
    get() = groups.size

  /** Recommended keeps thermal throttling and KSM alone; everything else is applied. */
  fun withPreset(preset: TweakPreset): TweakPrefs =
    when (preset) {
      TweakPreset.ALL -> copy(groups = TweakGroup.entries.toSet())
      TweakPreset.RECOMMENDED -> copy(groups = TweakGroup.entries.toSet() - TweakGroup.THERMAL)
      TweakPreset.NONE -> copy(groups = emptySet())
    }

  /** Clamp anything a stored preference or a migration could have pushed out of range. */
  fun sanitized(): TweakPrefs =
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
    val groups = TweakGroup.entries.filter { it in clean.groups }.joinToString(" ") { it.key }
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

/** Persists [TweakPrefs] per profile and writes the conf the engine sources. */
object TweakStore {
  private const val PREFS = "tweak_groups"

  fun load(
    context: Context,
    profile: Profile,
  ): TweakPrefs {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val prefix = "${profile.name.lowercase(Locale.US)}."
    // A profile saved before this feature existed has no keys: keep every group.
    val stored = TweakGroup.entries.filter { prefs.contains(prefix + it.key) }
    val groups =
      if (stored.isEmpty()) {
        TweakGroup.entries.toSet()
      } else {
        stored.filter { prefs.getBoolean(prefix + it.key, true) }.toSet()
      }

    fun int(key: String): Int? = if (prefs.contains(prefix + key)) prefs.getInt(prefix + key, 0) else null
    return TweakPrefs(
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
    prefs: TweakPrefs,
  ) {
    val clean = prefs.sanitized()
    val prefix = "${profile.name.lowercase(Locale.US)}."
    val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
    TweakGroup.entries.forEach { editor.putBoolean(prefix + it.key, it in clean.groups) }
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
      TweakEngine.directory(context),
      "custom/${profile.name.lowercase(Locale.US)}.conf",
    )

  /** Must run before every apply, including the boot reapply. */
  fun writeConf(
    context: Context,
    profile: Profile,
    prefs: TweakPrefs = load(context, profile),
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
