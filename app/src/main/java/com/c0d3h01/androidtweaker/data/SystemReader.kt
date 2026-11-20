package com.c0d3h01.androidtweaker.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.c0d3h01.androidtweaker.util.RootShell
import com.c0d3h01.androidtweaker.util.ShellAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Reads the live device snapshot from Android APIs and sysfs. */
object SystemReader {
  suspend fun read(context: Context): TelemetrySnapshot =
    withContext(Dispatchers.IO) {
      val root = ShellAccess.runnerOrNull()
      val meminfo =
        runCatching { File("/proc/meminfo").readText() }.getOrNull()
          ?: root?.let { runner -> runCatching { runner.run("cat /proc/meminfo") }.getOrNull() }
      val memPair = meminfo?.let { parseMeminfo(it) }
      val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
      val mi = ActivityManager.MemoryInfo()
      am?.getMemoryInfo(mi)
      val total = if (mi.totalMem > 0) mi.totalMem else memPair?.first ?: 0L
      val available = if (mi.totalMem > 0) mi.availMem else memPair?.second ?: 0L
      val swapTotal = meminfo?.let { kbValue(it, "SwapTotal") }?.times(1024)
      val swapFree = meminfo?.let { kbValue(it, "SwapFree") }?.times(1024)
      val zram = runCatching { File("/sys/block/zram0/disksize").readText().trim().toLong() }.getOrNull()

      TelemetrySnapshot(
        capturedAt = System.currentTimeMillis(),
        device = readDevice(),
        cpu = CpuInfo(readCores(root)),
        gpu = readGpu(root),
        thermal = readThermal(root),
        memory =
          if (total > 0) {
            MemoryStats(
              totalBytes = total,
              availableBytes = available,
              swapTotalBytes = swapTotal,
              swapFreeBytes = swapFree,
              zramBytes = zram,
            )
          } else {
            null
          },
        battery = readBattery(context),
        storage = readStorage(),
      )
    }

  private fun readDevice(): DeviceInfo {
    val soc =
      when {
        Build.VERSION.SDK_INT >= 31 -> Build.SOC_MODEL.ifBlank { null }
        else -> null
      } ?: cpuinfoHardware() ?: Build.HARDWARE
    return DeviceInfo(
      manufacturer = Build.MANUFACTURER?.replaceFirstChar { it.uppercase() },
      model = Build.MODEL ?: "Android device",
      soc = soc,
      android = Build.VERSION.RELEASE ?: "—",
      sdk = Build.VERSION.SDK_INT,
      abi = Build.SUPPORTED_ABIS.firstOrNull(),
      kernel = System.getProperty("os.version"),
    )
  }

  private fun cpuinfoHardware(): String? =
    runCatching {
      File("/proc/cpuinfo")
        .useLines { lines ->
          lines
            .firstOrNull { it.startsWith("Hardware", true) }
            ?.substringAfter(':')
            ?.trim()
            ?.ifBlank { null }
        }
    }.getOrNull()

  private fun readCores(root: ShellRunner?): List<CoreSample> {
    val rootDir = File("/sys/devices/system/cpu")
    val dirs =
      rootDir
        .listFiles()
        ?.filter { it.isDirectory && it.name.matches(Regex("cpu\\d+")) }
        ?.sortedBy { it.name.removePrefix("cpu").toIntOrNull() ?: 0 }
        .orEmpty()
    if (dirs.isEmpty()) return readCoresViaShell(root)
    return dirs
      .map { dir ->
        val index = dir.name.removePrefix("cpu").toIntOrNull() ?: return@map null
        val freq = File(dir, "cpufreq")
        val cur =
          normalizeKHz(
            fileLong(File(freq, "scaling_cur_freq"))
              ?: fileLong(File(freq, "cpuinfo_cur_freq")),
          )
        val min =
          normalizeKHz(
            fileLong(File(freq, "cpuinfo_min_freq"))
              ?: fileLong(File(freq, "scaling_min_freq")),
          )
        val max =
          normalizeKHz(
            fileLong(File(freq, "cpuinfo_max_freq"))
              ?: fileLong(File(freq, "scaling_max_freq")),
          )
        val gov = fileText(File(freq, "scaling_governor"))
        val online =
          if (index == 0) {
            true
          } else {
            fileText(File(dir, "online"))?.trim() != "0"
          }
        CoreSample(
          index = index,
          online = online,
          curKHz = cur?.takeIf { it > 0 },
          minKHz = min,
          maxKHz = max,
          governor = gov,
        )
      }.filterNotNull()
  }

  /**
   * Fallback for devices where SELinux hides /sys/devices/system/cpu from the
   * app process. The shared root shell sees the same nodes.
   */
  private fun readCoresViaShell(root: ShellRunner?): List<CoreSample> {
    if (root == null) return emptyList()
    val out =
      runCatching {
        root.run(
          """
          for d in /sys/devices/system/cpu/cpu[0-9]*; do
            [ -d "${'$'}d" ] || continue
            i=${'$'}{d##*/cpu}
            p=${'$'}d/cpufreq
            online=1
            [ "${'$'}i" != "0" ] && [ -r "${'$'}d/online" ] && online=${'$'}(cat "${'$'}d/online" 2>/dev/null)
            cur=${'$'}(cat "${'$'}p/scaling_cur_freq" 2>/dev/null || cat "${'$'}p/cpuinfo_cur_freq" 2>/dev/null)
            min=${'$'}(cat "${'$'}p/cpuinfo_min_freq" 2>/dev/null || cat "${'$'}p/scaling_min_freq" 2>/dev/null)
            max=${'$'}(cat "${'$'}p/cpuinfo_max_freq" 2>/dev/null || cat "${'$'}p/scaling_max_freq" 2>/dev/null)
            gov=${'$'}(cat "${'$'}p/scaling_governor" 2>/dev/null)
            echo "${'$'}i|${'$'}online|${'$'}cur|${'$'}min|${'$'}max|${'$'}gov"
          done
          """.trimIndent(),
        )
      }.getOrNull() ?: return emptyList()
    return out
      .lines()
      .mapNotNull { line ->
        val parts = line.split('|')
        if (parts.size < 6) return@mapNotNull null
        val index = parts[0].trim().toIntOrNull() ?: return@mapNotNull null
        CoreSample(
          index = index,
          online = parts[1].trim() != "0",
          curKHz = normalizeKHz(parts[2].trim().toLongOrNull())?.takeIf { it > 0 },
          minKHz = normalizeKHz(parts[3].trim().toLongOrNull()),
          maxKHz = normalizeKHz(parts[4].trim().toLongOrNull()),
          governor = parts[5].trim().ifBlank { null },
        )
      }.sortedBy { it.index }
  }

  private fun readGpu(root: ShellRunner?): GpuInfo? {
    val dir = findGpuDir()
    if (dir == null) {
      return root?.let { readGpuViaShell(it) ?: rendererOnly(it) } ?: rendererOnly(null)
    }
    val devfreq = File(dir, "devfreq").takeIf { it.isDirectory } ?: dir

    fun first(vararg names: String): Long? = names.firstNotNullOfOrNull { fileLong(File(devfreq, it)) }
    val cur =
      first("cur_freq", "scaling_cur_freq", "gpu_clock", "clk")
        ?: fileLong(File(dir, "cur_freq"))
        ?: fileLong(File(dir, "gpu_clock"))
    val min = first("min_freq", "scaling_min_freq")
    val max = first("max_freq", "scaling_max_freq")
    val governor =
      fileText(File(devfreq, "governor"))
        ?: fileText(File(dir, "gpu_governor"))
        ?: fileText(File(dir, "governor"))
    val model =
      fileText(File(dir, "gpu_model"))
        ?: fileText(File(dir, "model"))
        ?: guessGpuModel(dir)
    val busy =
      fileText(File(dir, "gpu_busy_percentage"))
        ?.filter { it.isDigit() }
        ?.toIntOrNull()
        ?: fileText(File(dir, "utilization"))
          ?.filter { it.isDigit() }
          ?.toIntOrNull()
        ?: fileText(File(dir, "load"))
          ?.filter { it.isDigit() }
          ?.toIntOrNull()
          ?.takeIf { it <= 100 }
        ?: fileText(File(dir, "gpubusy"))?.let(::gpuBusyPercent)
    val temp =
      fileLong(File(dir, "temp"))
        ?.let { normalizeTempC(it) }
    return GpuInfo(
      path = dir.absolutePath,
      model = model ?: renderer(root),
      curHz = toHz(cur),
      minHz = toHz(min),
      maxHz = toHz(max),
      governor = governor,
      busyPercent = busy?.takeIf { it in 0..100 },
      tempC = temp,
    )
  }

  private fun readGpuViaShell(root: ShellRunner): GpuInfo? {
    val out =
      runCatching {
        root.run(
          """
          for p in /sys/class/kgsl/kgsl-3d0 /sys/devices/platform/kgsl-3d0.0/kgsl/kgsl-3d0 /sys/kernel/gpu /sys/class/devfreq/* /sys/devices/platform/*gpu* /sys/devices/platform/*mali* /sys/devices/platform/soc/*gpu* /sys/devices/platform/soc/*mali*; do
            [ -d "${'$'}p" ] || continue
            case "${'$'}p" in *gpu*|*mali*|*kgsl*) ;; *) continue ;; esac
            echo "PATH=${'$'}p"
            for f in gpu_model model devfreq/cur_freq cur_freq devfreq/min_freq min_freq devfreq/max_freq max_freq devfreq/governor governor gpu_governor gpu_busy_percentage utilization load gpubusy gpu_clock clk temp; do
              [ -e "${'$'}p/${'$'}f" ] && echo "${'$'}f=${'$'}(cat "${'$'}p/${'$'}f" 2>/dev/null)"
            done
            break
          done
          """.trimIndent(),
        )
      }.getOrNull() ?: return null
    val fields =
      out
        .lines()
        .mapNotNull { line ->
          val at = line.indexOf('=')
          if (at <= 0) null else line.substring(0, at) to line.substring(at + 1).trim()
        }.toMap()
    if (fields.isEmpty()) return null
    val path = fields["PATH"]

    fun first(vararg keys: String): Long? = keys.firstNotNullOfOrNull { fields[it]?.toLongOrNull() }
    return GpuInfo(
      path = path,
      model =
        fields["gpu_model"]
          ?: fields["model"]
          ?: path?.let { p ->
            if (p.contains("kgsl")) {
              "Adreno"
            } else if (p.contains("mali")) {
              "Mali"
            } else {
              null
            }
          }
          ?: renderer(root),
      curHz = toHz(first("devfreq/cur_freq", "cur_freq", "gpu_clock", "clk")),
      minHz = toHz(first("devfreq/min_freq", "min_freq")),
      maxHz = toHz(first("devfreq/max_freq", "max_freq")),
      governor = fields["devfreq/governor"] ?: fields["gpu_governor"] ?: fields["governor"],
      busyPercent =
        (fields["gpu_busy_percentage"] ?: fields["utilization"] ?: fields["load"])
          ?.filter { it.isDigit() }
          ?.toIntOrNull()
          ?.takeIf { it in 0..100 }
          ?: fields["gpubusy"]?.let(::gpuBusyPercent),
      tempC = fields["temp"]?.toLongOrNull()?.let { normalizeTempC(it) },
    )
  }

  private fun findGpuDir(): File? {
    val direct =
      listOf(
        "/sys/class/kgsl/kgsl-3d0",
        "/sys/devices/platform/kgsl-3d0.0/kgsl/kgsl-3d0",
        "/sys/kernel/gpu",
        "/sys/class/misc/mali0/device",
        "/sys/devices/platform/gpusysfs",
        "/sys/devices/platform/soc/13000000.mali",
        "/sys/class/devfreq/mali",
      ).map(::File).firstOrNull { it.isDirectory }
    if (direct != null) return direct
    for (base in listOf("/sys/class/devfreq", "/sys/devices/platform", "/sys/devices/platform/soc")) {
      File(base)
        .listFiles()
        ?.firstOrNull { file ->
          val name = file.name.lowercase()
          name.contains("gpu") || name.contains("mali") || name.contains("kgsl")
        }?.let { dir ->
          File(dir, "devfreq").takeIf { it.isDirectory }?.let { return it }
          return dir
        }
    }
    return null
  }

  /** `gpubusy` holds "busy total"; everything else is a percentage already. */
  private fun gpuBusyPercent(raw: String): Int? {
    val parts = raw.trim().split(Regex("\\s+"))
    if (parts.size < 2) return null
    val busy = parts[0].toLongOrNull() ?: return null
    val total = parts[1].toLongOrNull() ?: return null
    if (total <= 0) return null
    return (busy * 100 / total).toInt().takeIf { it in 0..100 }
  }

  @Volatile private var gpuRendererCache: String? = null

  /** Renderer identity from SurfaceFlinger when the kernel exposes no GPU node. */
  private fun renderer(root: ShellRunner?): String? {
    gpuRendererCache?.let { return it }
    val runner = root ?: return null
    val out =
      runCatching {
        runner.run("dumpsys SurfaceFlinger 2>/dev/null | grep -m1 'GLES:' || getprop ro.hardware.egl")
      }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null
    val text =
      out.lineSequence().firstOrNull { it.contains("GLES:", true) }?.substringAfter("GLES:")?.trim()
        ?: out.lineSequence().firstOrNull { it.isNotBlank() }?.trim()
        ?: return null
    val model = gpuRendererName(text) ?: return null
    gpuRendererCache = model
    return model
  }

  private fun rendererOnly(root: ShellRunner?): GpuInfo? {
    val model = renderer(root) ?: return null
    return GpuInfo(
      path = null,
      model = model,
      curHz = null,
      minHz = null,
      maxHz = null,
      governor = null,
      busyPercent = null,
      tempC = null,
    )
  }

  /**
   * The GLES line is `vendor, renderer, version`, and the renderer itself nests
   * commas inside parentheses, so split on top-level commas only. Extension names
   * after the version (GL_APPLE_…) must never be mistaken for the renderer.
   */
  internal fun gpuRendererName(gles: String): String? {
    val text = gles.trim()
    if (text.isEmpty()) return null
    if (text.contains("SwiftShader", true)) return "SwiftShader (software)"
    val fields = mutableListOf(StringBuilder())
    var depth = 0
    for (ch in text) {
      when (ch) {
        '(' -> {
          depth++
          fields.last().append(ch)
        }

        ')' -> {
          depth = (depth - 1).coerceAtLeast(0)
          fields.last().append(ch)
        }

        ',' -> if (depth == 0) fields += StringBuilder() else fields.last().append(ch)

        else -> fields.last().append(ch)
      }
    }
    val renderer = (fields.getOrNull(1)?.toString() ?: text).trim()
    if (renderer.startsWith("Android Emulator", true)) {
      // "Android Emulator OpenGL ES Translator (Apple M5)" -> "Apple M5 (emulator)";
      // a wrapper such as ANGLE (…) names no hardware, so keep the generic label.
      val inner = renderer.substringAfter('(', "").substringBeforeLast(')').trim()
      val wrapper =
        inner.contains('(') ||
          inner.lowercase() in setOf("angle", "swiftshader", "llvmpipe", "mesa", "softpipe")
      return if (inner.isNotEmpty() && !wrapper) "$inner (emulator)" else "Android Emulator"
    }
    val cleaned = renderer.replace("(TM)", " ").replace(Regex("\\s+"), " ").trim()
    return cleaned
      .substringBefore('(')
      .trim()
      .ifBlank { text.substringBefore(',').trim() }
      .take(34)
      .ifBlank { null }
  }

  private fun guessGpuModel(dir: File): String? {
    val p = dir.absolutePath.lowercase()
    return when {
      p.contains("kgsl") -> "Adreno"
      p.contains("mali") -> "Mali"
      else -> null
    }
  }

  private fun readThermal(root: ShellRunner?): List<ThermalZone> {
    val zones =
      File("/sys/class/thermal")
        .listFiles()
        ?.filter { it.name.startsWith("thermal_zone") }
        .orEmpty()
    var readings =
      zones.mapNotNull { zone ->
        val type = fileText(File(zone, "type")) ?: return@mapNotNull null
        val raw = fileLong(File(zone, "temp")) ?: return@mapNotNull null
        val temp = normalizeTempC(raw) ?: return@mapNotNull null
        friendlyThermalName(type) to temp
      }
    if (readings.isEmpty() && root != null) {
      readings =
        runCatching {
          root.run(
            """
            for z in /sys/class/thermal/thermal_zone*; do
              t=${'$'}(cat "${'$'}z/type" 2>/dev/null)
              v=${'$'}(cat "${'$'}z/temp" 2>/dev/null)
              [ -n "${'$'}t" ] && [ -n "${'$'}v" ] && echo "${'$'}t|${'$'}v"
            done
            """.trimIndent(),
          )
        }.getOrDefault("")
          .lines()
          .mapNotNull { line ->
            val parts = line.split('|')
            if (parts.size < 2) return@mapNotNull null
            val temp = normalizeTempC(parts[1].trim().toLongOrNull() ?: return@mapNotNull null) ?: return@mapNotNull null
            friendlyThermalName(parts[0].trim()) to temp
          }
    }
    // Keep the hottest instance of each named zone, then show the useful ones first.
    val byName = linkedMapOf<String, Float>()
    for ((name, temp) in readings) {
      byName[name] = maxOf(byName[name] ?: Float.NEGATIVE_INFINITY, temp)
    }
    val priority = listOf("CPU 0", "CPU 1", "CPU", "SoC", "GPU", "Skin", "Battery", "Charger", "Board", "Modem", "Wi-Fi")
    return byName.entries
      .sortedWith(compareBy({ priority.indexOf(it.key).takeIf { i -> i >= 0 } ?: 99 }, { it.key }))
      .take(8)
      .map { ThermalZone(it.key, it.value) }
  }

  private fun readBattery(context: Context): BatteryStats {
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    val percent = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 }
    val currentRaw = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
    val currentMa = batteryCurrentMa(currentRaw)
    val sticky =
      context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val temp =
      sticky
        ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        ?.takeUnless { it == Int.MIN_VALUE }
        ?.let { it / 10f }
    val voltage = sticky?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)?.takeIf { it > 0 }
    val status =
      when (sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
        BatteryManager.BATTERY_STATUS_FULL -> "Full"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
        else -> null
      }
    val plugged =
      when (sticky?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
        BatteryManager.BATTERY_PLUGGED_AC -> "AC"
        BatteryManager.BATTERY_PLUGGED_USB -> "USB"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
        else -> null
      }
    return BatteryStats(
      percent = percent,
      tempC = temp,
      currentMa = currentMa,
      voltageMv = voltage,
      status = status,
      plugged = plugged,
    )
  }

  private fun readStorage(): StorageStats? =
    runCatching {
      val stat = StatFs(Environment.getDataDirectory().absolutePath)
      StorageStats(totalBytes = stat.totalBytes, availableBytes = stat.availableBytes)
    }.getOrNull()

  private fun toHz(raw: Long?): Long? =
    when {
      raw == null || raw <= 0 -> null
      raw > 10_000_000 -> raw
      raw > 10_000 -> raw * 1_000
      raw < 100 -> raw * 1_000_000_000
      else -> raw * 1_000_000
    }

  /**
   * CPU frequency nodes are normally kHz, but some kernels (and the Android
   * emulator) publish MHz or GHz. Normalise everything to kHz.
   */
  private fun normalizeKHz(raw: Long?): Long? =
    when {
      raw == null || raw <= 0 -> null
      raw < 100 -> raw * 1_000_000
      raw < 100_000 -> raw * 1_000
      raw <= 10_000_000 -> raw
      else -> raw / 1_000
    }

  private fun kbValue(
    text: String,
    key: String,
  ): Long? =
    Regex("^$key:\\s+(\\d+)", RegexOption.MULTILINE)
      .find(text)
      ?.groupValues
      ?.getOrNull(1)
      ?.toLongOrNull()

  private fun fileText(file: File): String? = runCatching { file.readText().trim().ifBlank { null } }.getOrNull()

  private fun fileLong(file: File): Long? = runCatching { file.readText().trim().toLongOrNull() }.getOrNull()
}

object RootReader {
  @Volatile private var cached: Pair<ShellRunner, RootInfo>? = null

  fun read(requestRoot: Boolean = false): RootInfo {
    if (requestRoot) {
      runCatching { RootShell.request() }.onFailure {
        return RootInfo(false, null, null, null, null, it.message)
      }
    }
    val runner = RootShell.runnerOrNull()
    if (runner == null) {
      cached = null
      return if (ShellAccess.mode() == ShellAccess.Mode.SHIZUKU_ROOT) {
        RootInfo(true, "Shizuku", null, null, null)
      } else {
        RootInfo(false, null, null, null, null, RootShell.lastError)
      }
    }
    cached?.takeIf { it.first === runner }?.let { return it.second }
    val managerScript =
      """
      printf 'selinux=%s\n' "${'$'}(getenforce 2>/dev/null)"
      printf 'su=%s\n' "${'$'}(command -v su 2>/dev/null)"
      if [ -d /data/adb/ksu ]; then
        echo "KernelSU"
        if [ -x /data/adb/ksu/bin/ksud ]; then /data/adb/ksu/bin/ksud -V 2>/dev/null; elif [ -x /data/adb/ksud ]; then /data/adb/ksud -V 2>/dev/null; fi
      elif [ -d /data/adb/magisk ]; then
        echo "Magisk"
        if [ -x /data/adb/magisk/magisk ]; then /data/adb/magisk/magisk -v 2>/dev/null; else magisk -v 2>/dev/null; fi
      elif [ -d /data/adb/ap ] || [ -d /data/adb/apatch ]; then
        echo "APatch"
        if [ -x /data/adb/apd ]; then /data/adb/apd -V 2>/dev/null; elif [ -x /data/adb/ap/bin/apd ]; then /data/adb/ap/bin/apd -V 2>/dev/null; fi
      else
        echo "Root"
      fi
      """.trimIndent()
    val result = runCatching { runner.run(managerScript).lines().map(String::trim) }
    val lines = result.getOrElse { return RootInfo(false, null, null, null, null, it.message) }
    val manager = lines.filter { it.isNotEmpty() && !it.startsWith("selinux=") && !it.startsWith("su=") }
    return RootInfo(
      granted = true,
      manager = manager.firstOrNull(),
      managerVersion = manager.getOrNull(1),
      selinux = lines.firstOrNull { it.startsWith("selinux=") }?.removePrefix("selinux=")?.ifBlank { null },
      suPath = lines.firstOrNull { it.startsWith("su=") }?.removePrefix("su=")?.ifBlank { null },
    ).also { cached = runner to it }
  }
}

enum class LogSource(
  val title: String,
) {
  TWEAKS("Tweaks"),
  DMESG("Kernel"),
  LOGCAT("System"),
}

fun LogSource.path(tweaksPath: String): String =
  when (this) {
    LogSource.TWEAKS -> tweaksPath
    LogSource.DMESG -> "dmesg"
    LogSource.LOGCAT -> "logcat"
  }

data class LogResult(
  val source: LogSource,
  val lines: List<String>,
  val missing: Boolean = false,
  val error: String? = null,
)

object LogReader {
  fun read(
    source: LogSource,
    runner: ShellRunner,
    tweaksPath: String,
    maxLines: Int = 400,
  ): LogResult {
    val command =
      when (source) {
        LogSource.TWEAKS -> {
          val path = TweakCommands.quote(tweaksPath)
          "if [ -f $path ]; then tail -n $maxLines $path; else echo @MISSING@; fi"
        }

        LogSource.DMESG -> {
          "dmesg 2>&1 | tail -n $maxLines"
        }

        LogSource.LOGCAT -> {
          "logcat -d -v threadtime -t $maxLines 2>&1"
        }
      }
    val raw =
      try {
        runner.run(command)
      } catch (e: Exception) {
        return LogResult(source, emptyList(), error = e.message ?: "Shell unavailable")
      }
    if (raw.contains("@MISSING@")) {
      return LogResult(source, emptyList(), missing = true)
    }
    val lines =
      raw
        .lineSequence()
        .map { it.trimEnd() }
        .filter { it.isNotBlank() }
        .toList()
        .takeLast(maxLines)
    val denied =
      lines.firstOrNull {
        it.contains("Operation not permitted", true) ||
          it.contains("Permission denied", true) ||
          it.contains("not found", true) && it.contains("dmesg", true)
      }
    return if (denied != null && lines.size <= 2) {
      LogResult(source, emptyList(), error = denied)
    } else {
      LogResult(source, lines)
    }
  }
}
