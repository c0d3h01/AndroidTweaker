package com.c0d3h01.androidtweaker.profiles

/**
 * Feature sections, mirroring the grouping the engine exposes in the UI:
 * core kernel work, memory, power, network, display, system rules.
 */
enum class ProfileSection(
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
 * core/common.sh matches, so the two lists must stay in step.
 */
enum class ProfileGroup(
  val key: String,
  val label: String,
  val blurb: String,
  val section: ProfileSection,
) {
  CPU("cpu", "CPU", "Governor, clock limits, boost and input timing", ProfileSection.CORE),
  GPU("gpu", "GPU", "Max clock, power level and idle timer", ProfileSection.CORE),
  KERNEL("kernel", "Kernel & boost", "cpufreq flags, cgroup shares, entropy, wakelocks", ProfileSection.CORE),
  SCHED("sched", "Scheduler", "Kernel scheduler features, latencies and tunables", ProfileSection.CORE),
  STUNE("stune", "CPU boost", "Top-app and foreground boost values", ProfileSection.CORE),
  IO("io", "Storage I/O", "Queue depth, read-ahead and merge policy", ProfileSection.CORE),
  VM("vm", "Memory", "Swappiness, dirty ratios and writeback", ProfileSection.MEMORY),
  FS("fs", "Filesystem", "Inotify and file-lease policy", ProfileSection.MEMORY),
  KSM("ksm", "KSM", "Kernel same-page merging", ProfileSection.MEMORY),
  THERMAL("thermal", "Thermal", "msm_thermal limits and power-efficient workqueues", ProfileSection.POWER),
  DOZE("doze", "Doze timings", "Aggressive idle constants on battery profiles", ProfileSection.POWER),
  NETWORK("network", "Network stack", "TCP timeouts and socket buffer sizes", ProfileSection.NETWORK),
  DISPLAY("display", "Display", "Double-tap to wake, touch boost and power hints", ProfileSection.DISPLAY),
  RENICE("renice", "Priorities", "Nice values for system processes", ProfileSection.SYSTEM),
  SYSTEM("system", "Doze & system", "Doze constants, logd, perfd and mpdecision", ProfileSection.SYSTEM),
}

/** Preset choices offered above the group list. */
enum class ProfilePreset(
  val label: String,
) {
  ALL("All"),
  RECOMMENDED("Recommended"),
  NONE("None"),
}
