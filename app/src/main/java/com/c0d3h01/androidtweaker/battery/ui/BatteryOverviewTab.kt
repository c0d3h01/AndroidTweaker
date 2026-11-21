package com.c0d3h01.androidtweaker.battery.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.c0d3h01.androidtweaker.BatteryMonitorService.MonitorStatus
import com.c0d3h01.androidtweaker.R
import com.c0d3h01.androidtweaker.battery.BatteryReading
import com.c0d3h01.androidtweaker.battery.BatterySample
import com.c0d3h01.androidtweaker.battery.BatterySession
import com.c0d3h01.androidtweaker.battery.BatterySettings
import com.c0d3h01.androidtweaker.battery.BatteryStatus
import com.c0d3h01.androidtweaker.battery.Plugged
import com.c0d3h01.androidtweaker.battery.analysis.estimateRemaining
import com.c0d3h01.androidtweaker.battery.analysis.estimateToFull
import com.c0d3h01.androidtweaker.battery.analysis.qualityTier
import com.c0d3h01.androidtweaker.battery.batteryPowerWatts
import com.c0d3h01.androidtweaker.battery.chargeSpeedLabel
import com.c0d3h01.androidtweaker.battery.formatCurrent
import com.c0d3h01.androidtweaker.battery.formatDuration
import com.c0d3h01.androidtweaker.battery.formatMah
import com.c0d3h01.androidtweaker.battery.formatPower
import com.c0d3h01.androidtweaker.battery.formatPowerValue
import com.c0d3h01.androidtweaker.battery.formatTemp
import com.c0d3h01.androidtweaker.battery.formatVoltage
import com.c0d3h01.androidtweaker.battery.plugLabel
import com.c0d3h01.androidtweaker.ui.components.Eyebrow
import com.c0d3h01.androidtweaker.ui.components.GhostButton
import com.c0d3h01.androidtweaker.ui.components.Hairline
import com.c0d3h01.androidtweaker.ui.components.InfoRow
import com.c0d3h01.androidtweaker.ui.components.Meter
import com.c0d3h01.androidtweaker.ui.components.MetricTile
import com.c0d3h01.androidtweaker.ui.components.Panel
import com.c0d3h01.androidtweaker.ui.components.Sparkline
import com.c0d3h01.androidtweaker.ui.components.StatusChip
import com.c0d3h01.androidtweaker.ui.components.TwoColumnGrid
import com.c0d3h01.androidtweaker.ui.theme.AppColors
import com.c0d3h01.androidtweaker.ui.theme.AppType

@Composable
internal fun HeroPanel(
  reading: BatteryReading?,
  samples: List<BatterySample>,
  settings: BatterySettings,
  monitorStatus: MonitorStatus,
  onToggleMonitor: (Boolean) -> Unit,
) {
  val level = reading?.level
  val tone =
    when {
      level == null -> AppColors.InkFaint
      level <= 15 -> AppColors.Rose
      level <= 30 -> AppColors.Amber
      reading.isCharging -> AppColors.Mint
      else -> AppColors.Mint
    }
  Panel(
    container = AppColors.PanelRaised,
    borderColor = tone.copy(alpha = 0.25f),
    contentPadding = PaddingValues(18.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Text("BATTERY", style = AppType.micro, color = AppColors.InkFaint)
        Text(
          reading?.status?.let { statusLabel(it, reading) } ?: "Reading…",
          style = MaterialTheme.typography.headlineSmall,
          color = AppColors.Ink,
        )
      }
      MonitorChip(settings.monitorEnabled, monitorStatus)
    }
    Spacer(Modifier.height(10.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
      BatteryGauge(level = level, charging = reading?.isCharging == true, tone = tone)
    }
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      HeroStat("POWER", formatPower(reading?.powerW), AppColors.Mint, Modifier.weight(1f))
      HeroStat("TEMPERATURE", formatTemp(reading?.tempC), tempTone(reading?.tempC), Modifier.weight(1f))
      HeroStat(
        if (reading?.isPlugged == true) "TO FULL" else "TIME LEFT",
        if (reading?.isPlugged == true) {
          formatDuration(reading?.let { estimateToFull(it) })
        } else {
          formatDuration(reading?.let { estimateRemaining(samples, it, System.currentTimeMillis()) })
        },
        AppColors.Lavender,
        Modifier.weight(1f),
      )
    }
    Spacer(Modifier.height(15.dp))
    Hairline()
    Spacer(Modifier.height(13.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        if (reading?.isPlugged == true) Icons.Outlined.BatteryChargingFull else Icons.Outlined.BatteryFull,
        contentDescription = null,
        tint = tone,
        modifier = Modifier.size(18.dp),
      )
      Spacer(Modifier.width(9.dp))
      Column(Modifier.weight(1f)) {
        Text(
          reading?.let { "${formatCurrent(it.currentMa)} · ${formatVoltage(it.voltageMv)}" } ?: "Reading battery…",
          style = AppType.monoStrong,
          color = AppColors.Ink,
        )
        Text(
          reading?.let { plugLabel(it.plugged) + " · " + chargeSpeedLabel(it.currentMa, it.powerW, it.plugged) } ?: "",
          style = MaterialTheme.typography.bodySmall,
          color = AppColors.InkMuted,
        )
      }
      GhostButton(
        stringResource(if (settings.monitorEnabled) R.string.battery_monitor_pause else R.string.battery_monitor_start),
        icon = if (settings.monitorEnabled) Icons.Outlined.RestartAlt else Icons.Outlined.NotificationsActive,
        onClick = { onToggleMonitor(!settings.monitorEnabled) },
        modifier = Modifier.width(112.dp),
      )
    }
  }
}

@Composable
internal fun BatteryGauge(
  level: Int?,
  charging: Boolean,
  tone: Color,
) {
  val fraction = ((level ?: 0) / 100f).coerceIn(0f, 1f)
  val sweep by animateFloatAsState(
    targetValue = fraction,
    animationSpec = tween(700, easing = androidx.compose.animation.core.FastOutSlowInEasing),
    label = "gaugeSweep",
  )
  val state =
    when {
      charging -> "charging"
      level == null -> "level unknown"
      level >= 100 -> "full"
      else -> "on battery"
    }
  Box(
    Modifier
      .size(206.dp)
      .clearAndSetSemantics {
        contentDescription =
          level?.let { "Battery $it percent, $state" } ?: "Battery level unknown, $state"
      },
    contentAlignment = Alignment.Center,
  ) {
    Canvas(Modifier.fillMaxSize()) {
      val stroke = 13.dp.toPx()
      val inset = stroke / 2 + 6.dp.toPx()
      val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
      drawArc(
        color = AppColors.Hairline,
        startAngle = 135f,
        sweepAngle = 270f,
        useCenter = false,
        topLeft = Offset(inset, inset),
        size = arcSize,
        style = Stroke(stroke, cap = StrokeCap.Round),
      )
      if (sweep > 0f) {
        drawArc(
          brush =
            Brush.sweepGradient(
              listOf(tone.copy(alpha = 0.55f), tone, if (charging) AppColors.Lavender else tone),
              center = Offset(size.width / 2, size.height / 2),
            ),
          startAngle = 135f,
          sweepAngle = 270f * sweep,
          useCenter = false,
          topLeft = Offset(inset, inset),
          size = arcSize,
          style = Stroke(stroke, cap = StrokeCap.Round),
        )
      }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Row(verticalAlignment = Alignment.Bottom) {
        Text(
          level?.toString() ?: "—",
          style = MaterialTheme.typography.displayLarge.copy(fontSize = 58.sp, lineHeight = 62.sp),
          color = AppColors.Ink,
        )
        Text(
          "%",
          style = MaterialTheme.typography.headlineMedium,
          color = AppColors.InkMuted,
          modifier = Modifier.padding(bottom = 9.dp),
        )
      }
      Spacer(Modifier.height(4.dp))
      StatusChip(
        when {
          charging -> "Charging"
          level == null -> "Reading…"
          level >= 100 -> "Full"
          else -> "On battery"
        },
        tone,
      )
    }
  }
}

@Composable
internal fun HeroStat(
  label: String,
  value: String,
  tone: Color,
  modifier: Modifier = Modifier,
) {
  Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Text(label, style = AppType.micro, color = AppColors.InkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Spacer(Modifier.height(5.dp))
    Text(value, style = AppType.dataSmall, color = tone, maxLines = 1, overflow = TextOverflow.Ellipsis)
  }
}

@Composable
internal fun MetricGrid(
  reading: BatteryReading?,
  samples: List<BatterySample>,
) {
  TwoColumnGrid(listOf("current", "voltage", "power", "temp")) { key ->
    when (key) {
      "current" -> {
        MetricTile(
          label = "Electric current",
          value = reading?.currentMa?.toString() ?: "—",
          unit = if (reading?.currentMa != null) "mA" else null,
          tone = AppColors.Mint,
          trend = samples.mapNotNull { it.currentMa?.toFloat() }.takeLast(30),
          footer = if (reading?.isCharging == true) "Going in" else "Draw",
        )
      }

      "voltage" -> {
        MetricTile(
          label = "Battery voltage",
          value = reading?.voltageMv?.let { "%.2f".format(it / 1000f) } ?: "—",
          unit = "V",
          tone = AppColors.Sky,
          trend = samples.mapNotNull { it.voltageMv?.let { v -> v / 1000f } }.takeLast(30),
          footer = "Live reading",
        )
      }

      "power" -> {
        MetricTile(
          label = "Power",
          value = formatPowerValue(reading?.powerW),
          unit = "W",
          footer = stringResource(when {
            reading?.powerW == null -> R.string.battery_power_unavailable
            reading.isPlugged -> R.string.battery_power_charging
            else -> R.string.battery_power_draw
          }),
          tone = AppColors.Lavender,
          trend =
            samples
              .mapNotNull { p ->
                val ma = p.currentMa ?: return@mapNotNull null
                val mv = p.voltageMv ?: return@mapNotNull null
                batteryPowerWatts(ma, mv)
              }.takeLast(30),
        )
      }

      else -> {
        MetricTile(
          label = "Battery temperature",
          value = reading?.tempC?.let { "%.1f".format(it) } ?: "—",
          unit = "°C",
          tone = tempTone(reading?.tempC),
          trend = samples.mapNotNull { it.tempC }.takeLast(30),
          footer = reading?.tempC?.let { tempLabel(it) },
        )
      }
    }
  }
}

@Composable
internal fun ChargingPanel(
  reading: BatteryReading?,
  sessions: List<BatterySession>,
) {
  val lastCharge = sessions.lastOrNull { it.charging }
  Panel {
    Eyebrow(label = "Charging") {
      StatusChip(
        reading?.let { if (it.isPlugged) chargeSpeedLabel(it.currentMa, it.powerW, it.plugged) else "Unplugged" } ?: "—",
        if (reading?.isPlugged == true) AppColors.Lavender else AppColors.InkFaint,
      )
    }
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.Bottom) {
      Text(
        reading?.takeIf { it.isPlugged }?.powerW?.let { "%.1f".format(it) } ?: "—",
        style = MaterialTheme.typography.displayMedium,
        color = AppColors.Ink,
      )
      Spacer(Modifier.width(6.dp))
      Text("W", style = AppType.micro, color = AppColors.InkFaint, modifier = Modifier.padding(bottom = 7.dp))
      Spacer(Modifier.weight(1f))
      Column(horizontalAlignment = Alignment.End) {
        Text(
          reading?.let { plugLabel(it.plugged) } ?: "—",
          style = AppType.mono,
          color = AppColors.InkMuted,
        )
        Text(
          reading?.let { if (it.isPlugged) estimateToFull(it)?.let(::formatDuration) ?: "Measuring…" else "Not charging" } ?: "",
          style = AppType.micro,
          color = AppColors.InkFaint,
        )
      }
    }
    Spacer(Modifier.height(10.dp))
    Meter(
      fraction = (reading?.level ?: 0) / 100f,
      tone = if (reading?.isPlugged == true) AppColors.Mint else AppColors.Lavender,
    )
    Spacer(Modifier.height(14.dp))
    if (lastCharge != null) {
      Text("LAST CHARGE", style = AppType.micro, color = AppColors.InkFaint)
      Spacer(Modifier.height(8.dp))
      InfoRow(
        "Session",
        "${lastCharge.startLevel}% → ${lastCharge.endLevel}% · ${formatDuration(lastCharge.durationMs)}",
      )
      Spacer(Modifier.height(7.dp))
      InfoRow("Added", formatMah(lastCharge.addedMah.let { if (it > 0) it else null }))
      Spacer(Modifier.height(7.dp))
      InfoRow("Peak power", formatPower(lastCharge.peakPowerW))
      Spacer(Modifier.height(7.dp))
      InfoRow("Quality", "${lastCharge.quality} · ${qualityTier(lastCharge.quality)}", qualityTone(lastCharge.quality))
    } else {
      Text(
        "Plug in and the live current, power and time to full appear here. A charge summary is posted when you unplug.",
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.InkMuted,
      )
    }
  }
}

@Composable
internal fun TemperaturePanel(samples: List<BatterySample>) {
  val temps = samples.mapNotNull { it.tempC }
  Panel {
    Eyebrow(label = "Temperature · 24h") {
      if (temps.isNotEmpty()) {
        StatusChip("%.1f–%.1f\u00A0°C".format(temps.min(), temps.max()), tempTone(temps.max()))
      }
    }
    Spacer(Modifier.height(12.dp))
    if (temps.size < 2) {
      Text("Temperature history builds while the live monitor runs.", style = MaterialTheme.typography.bodySmall, color = AppColors.InkMuted)
    } else {
      Sparkline(
        temps.takeLast(160),
        AppColors.Sky,
        Modifier.fillMaxWidth().height(72.dp),
        fill = temps.max() - temps.min() > 0.2f,
        description = "Battery temperature trend, 24 hours",
      )
      Spacer(Modifier.height(9.dp))
      Row(Modifier.fillMaxWidth()) {
        Text("MIN %.1f\u00A0°C".format(temps.min()), style = AppType.micro, color = AppColors.InkFaint)
        Spacer(Modifier.weight(1f))
        Text("MAX %.1f\u00A0°C".format(temps.max()), style = AppType.micro, color = AppColors.InkFaint)
      }
    }
  }
}

internal fun statusLabel(
  status: BatteryStatus,
  reading: BatteryReading,
): String =
  when (status) {
    BatteryStatus.CHARGING -> "Charging"
    BatteryStatus.DISCHARGING -> "Discharging"
    BatteryStatus.FULL -> "Fully charged"
    BatteryStatus.IDLE -> "Not charging"
    BatteryStatus.UNKNOWN -> if (reading.isPlugged) "Plugged in" else "On battery"
  }

internal fun tempTone(c: Float?): Color =
  when {
    c == null -> AppColors.InkMuted
    c >= 60f -> AppColors.Rose
    c >= 45f -> AppColors.Amber
    c <= 5f -> AppColors.Sky
    else -> AppColors.Mint
  }

internal fun tempLabel(c: Float): String =
  when {
    c >= 60f -> "Very hot"
    c >= 45f -> "Warm"
    c <= 5f -> "Cold"
    else -> "Normal"
  }
