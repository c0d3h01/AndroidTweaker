package com.c0d3h01.androidtweaker.access

object RootReader {
  @Volatile private var cached: Pair<CommandRunner, RootInfo>? = null

  fun read(requestRoot: Boolean = false): RootInfo {
    if (requestRoot) {
      runCatching { RootConnection.request() }.onFailure {
        return RootInfo(false, null, null, null, null, it.message)
      }
    }
    val runner = RootConnection.runnerOrNull()
    if (runner == null) {
      cached = null
      return if (AccessManager.mode() == AccessManager.Mode.SHIZUKU_ROOT) {
        RootInfo(true, "Shizuku", null, null, null)
      } else {
        RootInfo(false, null, null, null, null, RootConnection.lastError)
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
