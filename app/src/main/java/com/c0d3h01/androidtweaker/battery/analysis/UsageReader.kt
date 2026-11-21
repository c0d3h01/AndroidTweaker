package com.c0d3h01.androidtweaker.battery.analysis

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import com.c0d3h01.androidtweaker.battery.AppUsage

object UsageReader {
  fun hasAccess(context: Context): Boolean {
    val ops = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
    val mode =
      if (Build.VERSION.SDK_INT >= 29) {
        ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
      } else {
        @Suppress("DEPRECATION")
        ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
      }
    return mode == AppOpsManager.MODE_ALLOWED
  }

  fun apps(
    context: Context,
    since: Long,
    limit: Int = 8,
  ): List<AppUsage> {
    if (!hasAccess(context)) return emptyList()
    val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyList()
    val now = System.currentTimeMillis()
    val stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, since, now).orEmpty()
    val grouped = linkedMapOf<String, Pair<Long, Int>>()
    for (stat in stats) {
      val previous = grouped[stat.packageName]
      grouped[stat.packageName] = (previous?.first ?: 0L) + stat.totalTimeInForeground to
        (previous?.second ?: 0) + stat.lastTimeUsed.let { if (it >= since) 1 else 0 }
    }
    val total = grouped.values.sumOf { it.first }.coerceAtLeast(1L)
    val pm = context.packageManager
    return grouped.entries
      .filter { it.value.first >= 30_000 }
      .sortedByDescending { it.value.first }
      .take(limit)
      .map { (pkg, value) ->
        val label =
          runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
          }.getOrDefault(pkg.substringAfterLast('.'))
        AppUsage(pkg, label, value.first, value.second, value.first.toFloat() / total)
      }
  }
}
