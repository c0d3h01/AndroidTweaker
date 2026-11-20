package com.c0d3h01.androidtweaker.device

import java.util.Locale

/** Pure data models for the live instrument screens. */
data class DeviceInfo(
  val manufacturer: String?,
  val model: String,
  val soc: String?,
  val android: String,
  val sdk: Int,
  val abi: String?,
  val kernel: String?,
)

data class CoreSample(
  val index: Int,
  val online: Boolean,
  val curKHz: Long?,
  val minKHz: Long?,
  val maxKHz: Long?,
  val governor: String?,
)

data class CpuCluster(
  val label: String,
  val cores: List<CoreSample>,
  val governor: String?,
  val curKHz: Long?,
  val minKHz: Long?,
  val maxKHz: Long?,
)

data class CpuInfo(
  val cores: List<CoreSample>,
) {
  val clusters: List<CpuCluster> = clusterCores(cores)
}

/** Stable identity for a cluster across samples. */
val CpuCluster.key: String
  get() = "$label-${cores.firstOrNull()?.index ?: 0}"

data class GpuInfo(
  val path: String?,
  val model: String?,
  val curHz: Long?,
  val minHz: Long?,
  val maxHz: Long?,
  val governor: String?,
  val busyPercent: Int?,
  val tempC: Float?,
)

data class ThermalZone(
  val name: String,
  val tempC: Float,
)

data class MemoryStats(
  val totalBytes: Long,
  val availableBytes: Long,
  val swapTotalBytes: Long?,
  val swapFreeBytes: Long?,
  val zramBytes: Long?,
) {
  val usedBytes: Long get() = (totalBytes - availableBytes).coerceAtLeast(0)
  val usedFraction: Float
    get() = if (totalBytes > 0) usedBytes.toFloat() / totalBytes else 0f
}

data class BatteryStats(
  val percent: Int?,
  val tempC: Float?,
  val currentMa: Int?,
  val voltageMv: Int?,
  val status: String?,
  val plugged: String?,
)

data class StorageStats(
  val totalBytes: Long,
  val availableBytes: Long,
) {
  val usedFraction: Float
    get() = if (totalBytes > 0) (totalBytes - availableBytes).toFloat() / totalBytes else 0f
}

data class TelemetrySnapshot(
  val capturedAt: Long,
  val device: DeviceInfo,
  val cpu: CpuInfo,
  val gpu: GpuInfo?,
  val thermal: List<ThermalZone>,
  val memory: MemoryStats?,
  val battery: BatteryStats,
  val storage: StorageStats?,
)

/**
 * Groups cores into clusters by walking the core list in order and starting a
 * new group whenever the frequency envelope changes. Pure so it can be tested
 * without an Android device.
 */
fun clusterCores(cores: List<CoreSample>): List<CpuCluster> {
  if (cores.isEmpty()) return emptyList()
  val groups = mutableListOf<MutableList<CoreSample>>()
  for (core in cores.sortedBy { it.index }) {
    val current = groups.lastOrNull()
    val key = "${core.minKHz}:${core.maxKHz}"
    val currentKey = current?.firstOrNull()?.let { "${it.minKHz}:${it.maxKHz}" }
    if (current != null && key == currentKey) current.add(core) else groups.add(mutableListOf(core))
  }
  val labels =
    when (groups.size) {
      1 -> {
        listOf("CLUSTER")
      }

      2 -> {
        listOf("LITTLE", "BIG")
      }

      3 -> {
        listOf("LITTLE", "MID", "PRIME")
      }

      else -> {
        List(groups.size) { i ->
          when (i) {
            0 -> "LITTLE"
            groups.lastIndex -> "PRIME"
            else -> "MID $i"
          }
        }
      }
    }
  return groups.mapIndexed { index, group ->
    CpuCluster(
      label = labels.getOrElse(index) { "CLUSTER ${index + 1}" },
      cores = group,
      governor = group.mapNotNull { it.governor }.firstOrNull(),
      curKHz = group.mapNotNull { it.curKHz }.maxOrNull(),
      minKHz = group.minOfOrNull { it.minKHz ?: Long.MAX_VALUE }?.takeUnless { it == Long.MAX_VALUE },
      maxKHz = group.mapNotNull { it.maxKHz }.maxOrNull(),
    )
  }
}

/** Turns "cpu-0-0-usr", "soc-thermal", "battery" into a short display name. */
fun friendlyThermalName(raw: String): String {
  val s = raw.lowercase().replace('_', '-')
  return when {
    s.contains("cpu") && s.contains("0") -> {
      "CPU 0"
    }

    s.contains("cpu") && s.contains("1") -> {
      "CPU 1"
    }

    s.contains("cpu") -> {
      "CPU"
    }

    s.contains("gpu") -> {
      "GPU"
    }

    s.contains("battery") -> {
      "Battery"
    }

    s.contains("skin") -> {
      "Skin"
    }

    s.contains("soc") || s.contains("socpm") -> {
      "SoC"
    }

    s.contains("pmic") -> {
      "PMIC"
    }

    s.contains("charger") || s.contains("chg") -> {
      "Charger"
    }

    s.contains("usb") || s.contains("xo") -> {
      "Board"
    }

    s.contains("modem") || s.contains("mdm") -> {
      "Modem"
    }

    s.contains("wifi") || s.contains("wlan") -> {
      "Wi-Fi"
    }

    s.contains("quiet") -> {
      "Quiet"
    }

    s.contains("nsp") -> {
      "NSP"
    }

    else -> {
      raw
        .replace('_', ' ')
        .replace('-', ' ')
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
    }
  }
}

/** Parses /proc/meminfo values expressed in kB into bytes. */
fun parseMeminfo(text: String): Pair<Long, Long>? {
  fun kb(key: String): Long? =
    Regex("^$key:\\s+(\\d+)", RegexOption.MULTILINE)
      .find(text)
      ?.groupValues
      ?.getOrNull(1)
      ?.toLongOrNull()
  val total = kb("MemTotal") ?: return null
  val available = kb("MemAvailable") ?: kb("MemFree") ?: return null
  return total * 1024 to available * 1024
}

fun formatHz(hz: Long?): String =
  when {
    hz == null -> "—"
    hz >= 1_000_000_000 -> String.format(Locale.US, "%.2f\u00A0GHz", hz / 1_000_000_000.0)
    hz >= 1_000_000 -> String.format(Locale.US, "%.0f\u00A0MHz", hz / 1_000_000.0)
    hz >= 1_000 -> String.format(Locale.US, "%.0f\u00A0kHz", hz / 1_000.0)
    else -> "$hz\u00A0Hz"
  }

fun formatKHz(khz: Long?): String =
  when {
    khz == null -> "—"
    khz >= 1_000_000 -> String.format(Locale.US, "%.2f\u00A0GHz", khz / 1_000_000.0)
    else -> String.format(Locale.US, "%.0f\u00A0MHz", khz / 1_000.0)
  }

fun formatBytes(bytes: Long?): String =
  when {
    bytes == null -> "—"
    bytes >= 1L shl 30 -> String.format(Locale.US, "%.1f\u00A0GB", bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 -> String.format(Locale.US, "%.0f\u00A0MB", bytes / (1L shl 20).toDouble())
    else -> String.format(Locale.US, "%.0f\u00A0KB", bytes / 1024.0)
  }

fun profileLevel(code: Int?): Int =
  when (code) {
    1 -> 1
    2 -> 2
    3 -> 3
    4 -> 4
    else -> 0
  }
