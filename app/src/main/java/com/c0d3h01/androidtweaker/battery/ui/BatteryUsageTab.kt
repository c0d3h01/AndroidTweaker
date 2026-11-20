package com.c0d3h01.androidtweaker.battery.ui

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.c0d3h01.androidtweaker.battery.AppUsage
import com.c0d3h01.androidtweaker.battery.BatteryReading
import com.c0d3h01.androidtweaker.battery.BatterySample
import com.c0d3h01.androidtweaker.battery.BatterySession
import com.c0d3h01.androidtweaker.battery.BatterySettings
import com.c0d3h01.androidtweaker.battery.analysis.drainRate
import com.c0d3h01.androidtweaker.battery.analysis.estimateRemaining
import com.c0d3h01.androidtweaker.battery.analysis.screenStatsOf
import com.c0d3h01.androidtweaker.battery.formatDuration
import com.c0d3h01.androidtweaker.ui.components.Eyebrow
import com.c0d3h01.androidtweaker.ui.components.GhostButton
import com.c0d3h01.androidtweaker.ui.components.Hairline
import com.c0d3h01.androidtweaker.ui.components.InfoRow
import com.c0d3h01.androidtweaker.ui.components.Meter
import com.c0d3h01.androidtweaker.ui.components.Panel
import com.c0d3h01.androidtweaker.ui.components.StatusChip
import com.c0d3h01.androidtweaker.ui.components.SwitchRow
import com.c0d3h01.androidtweaker.ui.theme.AppColors
import com.c0d3h01.androidtweaker.ui.theme.AppType

@Composable
internal fun UsageOverview(
  sessions: List<BatterySession>,
  reading: BatteryReading?,
  samples: List<BatterySample>,
) {
  val day = sessions.filter { it.end >= System.currentTimeMillis() - 24 * 3_600_000L }
  val stats = screenStatsOf(day)
  val total = (stats.screenOnMs + stats.screenOffMs).coerceAtLeast(1)
  val screenOnRate = drainRate(samples, true, System.currentTimeMillis())
  val screenOffRate = drainRate(samples, false, System.currentTimeMillis())
  Panel {
    Eyebrow(label = "Screen & sleep · 24h") {
      StatusChip(if (stats.screenOnMs + stats.screenOffMs > 0) "Tracked" else "Collecting", AppColors.Sky)
    }
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      StatBox("SCREEN ON", formatDuration(stats.screenOnMs), AppColors.Mint, Modifier.weight(1f))
      StatBox("SCREEN OFF", formatDuration(stats.screenOffMs), AppColors.InkMuted, Modifier.weight(1f))
    }
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      StatBox("DEEP SLEEP", formatDuration(stats.deepSleepMs), AppColors.Lavender, Modifier.weight(1f))
      StatBox("AWAKE", formatDuration(stats.awakeMs), AppColors.Amber, Modifier.weight(1f))
    }
    Spacer(Modifier.height(13.dp))
    Meter(
      fraction = if (total > 0) stats.screenOnMs.toFloat() / total else 0f,
      tone = AppColors.Mint,
    )
    Spacer(Modifier.height(7.dp))
    Text(
      if (total >
        0
      ) {
        "%.0f%% of the last day had the screen on".format(stats.screenOnMs * 100f / total)
      } else {
        "Screen stats build while the monitor runs."
      },
      style = MaterialTheme.typography.bodySmall,
      color = AppColors.InkMuted,
    )
    Spacer(Modifier.height(14.dp))
    Hairline()
    Spacer(Modifier.height(13.dp))
    InfoRow(
      "Drain · screen on",
      screenOnRate?.let { "%.1f%%/h".format(it) } ?: "Measuring…",
      AppColors.Mint,
    )
    Spacer(Modifier.height(7.dp))
    InfoRow(
      "Drain · screen off",
      screenOffRate?.let { "%.1f%%/h".format(it) } ?: "Measuring…",
      AppColors.Lavender,
    )
    Spacer(Modifier.height(7.dp))
    InfoRow(
      "Estimated left",
      formatDuration(reading?.let { estimateRemaining(samples, it, System.currentTimeMillis()) }),
      AppColors.Sky,
    )
  }
}

@Composable
internal fun StatBox(
  label: String,
  value: String,
  tone: Color,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier
      .clip(RoundedCornerShape(14.dp))
      .background(AppColors.Sunken)
      .border(1.dp, AppColors.Hairline, RoundedCornerShape(14.dp))
      .padding(horizontal = 12.dp, vertical = 11.dp),
  ) {
    Text(label, style = AppType.micro, color = AppColors.InkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Spacer(Modifier.height(4.dp))
    Text(value, style = AppType.dataSmall, color = tone)
  }
}

@Composable
internal fun AppUsagePanel(
  apps: List<AppUsage>,
  access: Boolean,
  onOpenSettings: () -> Unit,
) {
  Panel {
    Eyebrow(label = "App usage") {
      if (access) Text("SINCE UNPLUG", style = AppType.micro, color = AppColors.InkFaint)
    }
    Spacer(Modifier.height(10.dp))
    if (!access) {
      Text(
        "Grant Usage access to see which apps used the screen and how much of the session each one took.",
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.InkMuted,
      )
      Spacer(Modifier.height(12.dp))
      GhostButton("Allow usage access", Icons.Outlined.Settings, onClick = onOpenSettings)
    } else if (apps.isEmpty()) {
      Text(
        "No app usage yet. Unplug and use the phone for a few minutes.",
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.InkMuted,
      )
    } else {
      apps.forEachIndexed { index, app ->
        if (index > 0) {
          Spacer(Modifier.height(11.dp))
          Hairline()
          Spacer(Modifier.height(11.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(AppColors.Signal.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              app.label.take(1).uppercase(),
              style = AppType.monoStrong,
              color = AppColors.Signal,
            )
          }
          Spacer(Modifier.width(10.dp))
          Column(Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.titleMedium, color = AppColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
              "${formatDuration(app.screenMs)} on screen",
              style = MaterialTheme.typography.bodySmall,
              color = AppColors.InkMuted,
            )
          }
          Column(horizontalAlignment = Alignment.End) {
            Text("%.0f%%".format(app.share * 100f), style = AppType.monoStrong, color = AppColors.Mint)
            Text("${app.launches} starts", style = AppType.micro, color = AppColors.InkFaint)
          }
        }
        Spacer(Modifier.height(7.dp))
        Meter(app.share.coerceIn(0f, 1f), AppColors.Mint, height = 3.dp)
      }
    }
  }
}

@Composable
internal fun AnomalyPanel(
  settings: BatterySettings,
  update: ((BatterySettings) -> BatterySettings) -> Unit,
) {
  Panel {
    Eyebrow(label = "Drain alerts")
    Spacer(Modifier.height(8.dp))
    SwitchRow(
      title = "Abnormal drain alert",
      blurb = "Warn when screen-off drain is far above usual",
      icon = Icons.Outlined.Info,
      tone = AppColors.Rose,
      checked = settings.anomalyAlert,
    ) { update { it.copy(anomalyAlert = !it.anomalyAlert) } }
    Text(
      "Measured on this device from your own sessions. Requires the live monitor to be running.",
      style = MaterialTheme.typography.bodySmall,
      color = AppColors.InkFaint,
    )
  }
}

@Composable
internal fun TipPanel(
  title: String,
  tips: List<String>,
) {
  Panel {
    Eyebrow(label = title)
    Spacer(Modifier.height(10.dp))
    tips.forEachIndexed { index, tip ->
      Row(verticalAlignment = Alignment.Top) {
        Text("${index + 1}", style = AppType.micro, color = AppColors.Signal, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.width(9.dp))
        Text(tip, style = MaterialTheme.typography.bodySmall, color = AppColors.InkMuted)
      }
      if (index != tips.lastIndex) Spacer(Modifier.height(8.dp))
    }
  }
}

internal fun openUsageAccess(context: android.content.Context) {
  val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
  runCatching { context.startActivity(intent) }
    .onFailure { Toast.makeText(context, "Open Settings → Usage access", Toast.LENGTH_SHORT).show() }
}
