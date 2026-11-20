package com.c0d3h01.androidtweaker.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.c0d3h01.androidtweaker.BatteryMonitorService
import com.c0d3h01.androidtweaker.BatteryMonitorService.MonitorStatus
import com.c0d3h01.androidtweaker.R
import com.c0d3h01.androidtweaker.data.AppUsage
import com.c0d3h01.androidtweaker.data.BatteryReader
import com.c0d3h01.androidtweaker.data.BatteryReading
import com.c0d3h01.androidtweaker.data.BatterySample
import com.c0d3h01.androidtweaker.data.BatterySession
import com.c0d3h01.androidtweaker.data.BatterySettings
import com.c0d3h01.androidtweaker.data.BatteryStatus
import com.c0d3h01.androidtweaker.data.BatteryStore
import com.c0d3h01.androidtweaker.data.BatterySummary
import com.c0d3h01.androidtweaker.data.Plugged
import com.c0d3h01.androidtweaker.data.UsageReader
import com.c0d3h01.androidtweaker.data.chargeSpeedLabel
import com.c0d3h01.androidtweaker.data.drainRate
import com.c0d3h01.androidtweaker.data.estimateHealth
import com.c0d3h01.androidtweaker.data.estimateRemaining
import com.c0d3h01.androidtweaker.data.estimateToFull
import com.c0d3h01.androidtweaker.data.formatCurrent
import com.c0d3h01.androidtweaker.data.formatDuration
import com.c0d3h01.androidtweaker.data.formatMah
import com.c0d3h01.androidtweaker.data.formatPower
import com.c0d3h01.androidtweaker.data.formatTemp
import com.c0d3h01.androidtweaker.data.formatVoltage
import com.c0d3h01.androidtweaker.data.plugLabel
import com.c0d3h01.androidtweaker.data.qualityTier
import com.c0d3h01.androidtweaker.data.screenStatsOf
import com.c0d3h01.androidtweaker.util.dateClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val TABS = listOf("Overview", "Health", "Usage", "History")

@Composable
fun BatteryScreen(onRequestNotifications: () -> Unit = {}) {
  val context = LocalContext.current
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  val monitorStatus by BatteryMonitorService.status.collectAsStateWithLifecycle()
  var reading by remember { mutableStateOf<BatteryReading?>(null) }
  var settings by remember { mutableStateOf(BatteryStore.settings(context)) }
  var sessions by remember { mutableStateOf(BatteryStore.sessions(context)) }
  var samples by remember { mutableStateOf(BatteryStore.samples(context)) }
  var apps by remember { mutableStateOf<List<AppUsage>>(emptyList()) }
  var usageAccess by remember { mutableStateOf(UsageReader.hasAccess(context)) }
  var tab by rememberSaveable { mutableIntStateOf(0) }
  var confirmDelete by remember { mutableStateOf(false) }

  LaunchedEffect(context, lifecycle) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
      while (true) {
        reading = withContext(Dispatchers.IO) { runCatching { BatteryReader.read(context) }.getOrNull() }
        settings = BatteryStore.settings(context)
        sessions = BatteryStore.sessions(context)
        samples = BatteryStore.samples(context)
        usageAccess = UsageReader.hasAccess(context)
        delay(2_500)
      }
    }
  }

  LaunchedEffect(tab, sessions.size, usageAccess) {
    if (tab != 2 || !usageAccess) return@LaunchedEffect
    apps =
      withContext(Dispatchers.IO) {
        val since = sessions.lastOrNull { it.charging }?.end ?: (System.currentTimeMillis() - 6 * 3_600_000L)
        UsageReader.apps(context, since)
      }
  }

  fun update(change: (BatterySettings) -> BatterySettings) {
    settings = BatteryStore.update(context, change)
  }

  fun setMonitor(on: Boolean) {
    if (on) {
      onRequestNotifications()
      BatteryStore.update(context) { it.copy(monitorEnabled = true) }
      BatteryMonitorService.start(context)
    } else {
      BatteryMonitorService.stop(context)
    }
    settings = BatteryStore.settings(context)
  }

  fun share(
    file: File,
    mime: String,
    title: String,
  ) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    context.startActivity(Intent.createChooser(intent, title))
  }

  val health = estimateHealth(sessions, reading?.designMah, reading?.fullMah, reading?.cycles)
  val week = remember(sessions) { BatteryStore.summarySince(context, System.currentTimeMillis() - 7 * 24 * 3_600_000L) }
  val statusTone =
    when {
      reading == null -> Bench.InkFaint
      (reading?.level ?: 100) <= 15 -> Bench.Rose
      reading?.isCharging == true -> Bench.Mint
      else -> Bench.Lavender
    }

  LazyColumn(
    Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = Bench.Gutter, end = Bench.Gutter, top = 12.dp, bottom = Bench.BarClearance),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item { Wordmark() }
    item {
      ScreenTitle(title = "Battery", subtitle = "Gauge, health & usage") {
        StatusChip(
          when {
            reading == null -> "Reading…"
            reading?.isCharging == true -> "Charging"
            else -> "On battery"
          },
          statusTone,
        )
      }
    }
    item { BenchTabStrip(labels = TABS, selected = tab, onSelect = { tab = it }) }

    when (tab) {
      0 -> {
        item { HeroPanel(reading, samples, settings, monitorStatus, onToggleMonitor = ::setMonitor) }
        item { MetricGrid(reading, samples) }
        item { ChargingPanel(reading, sessions) }
        item { TemperaturePanel(samples) }
        item { AlarmPanel(settings, ::update) }
        item { MonitorPanel(settings, monitorStatus, ::update) }
      }

      1 -> {
        item { HealthPanel(health, reading, sessions) }
        item { HealthChart(sessions, health.designMah) }
        item { ChargeSessions(sessions) }
        item {
          InfoNote(
            "Health is estimated from your own charges. A charge that adds at least 15% gives a usable reading; closer to full is better. Phone-reported capacity is used when the system exposes it.",
          )
        }
      }

      2 -> {
        item { UsageOverview(sessions, reading, samples) }
        item { AppUsagePanel(apps, usageAccess, onOpenSettings = { openUsageAccess(context) }) }
        item {
          TipPanel(
            "Battery care",
            listOf(
              "Keep the battery between 20% and 80% when convenient.",
              "Avoid long periods above 45\u00A0°C, especially while charging.",
              "Fast charging is fine for top-ups; charge slower overnight when you can.",
              "Deep sleep matters more than total screen-off time.",
            ),
          )
        }
        item { AnomalyPanel(settings, ::update) }
      }

      else -> {
        item { WeekPanel(week, sessions) }
        item { SessionHistory(sessions) }
        item {
          DataPanel(
            onCsv = { share(BatteryStore.exportCsv(context), "text/csv", "Battery CSV") },
            onBackup = { share(BatteryStore.backupJson(context), "application/json", "Battery backup") },
            onDelete = { confirmDelete = true },
          )
        }
        item {
          InfoNote(
            "CSV and backup files are created on this device and leave only through the share sheet. No telemetry is uploaded.",
          )
        }
      }
    }
  }

  if (confirmDelete) {
    AlertDialog(
      onDismissRequest = { confirmDelete = false },
      containerColor = Bench.Panel,
      titleContentColor = Bench.Ink,
      textContentColor = Bench.InkMuted,
      shape = RoundedCornerShape(20.dp),
      title = { Text("Delete battery history?") },
      text = { Text("This deletes all samples and sessions. Settings and alarms stay. You can't undo it.") },
      confirmButton = {
        TextButton(onClick = {
          BatteryStore.deleteHistory(context)
          sessions = BatteryStore.sessions(context)
          samples = BatteryStore.samples(context)
          confirmDelete = false
        }) {
          Text("Delete", color = Bench.Rose)
        }
      },
      dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = Bench.InkMuted) } },
    )
  }
}

@Composable
private fun HeroPanel(
  reading: BatteryReading?,
  samples: List<BatterySample>,
  settings: BatterySettings,
  monitorStatus: MonitorStatus,
  onToggleMonitor: (Boolean) -> Unit,
) {
  val level = reading?.level
  val tone =
    when {
      level == null -> Bench.InkFaint
      level <= 15 -> Bench.Rose
      level <= 30 -> Bench.Amber
      reading.isCharging -> Bench.Mint
      else -> Bench.Mint
    }
  Panel(
    container = Bench.PanelRaised,
    borderColor = tone.copy(alpha = 0.25f),
    contentPadding = PaddingValues(18.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) {
        Text("BATTERY", style = AppType.micro, color = Bench.InkFaint)
        Text(
          reading?.status?.let { statusLabel(it, reading) } ?: "Reading…",
          style = MaterialTheme.typography.headlineSmall,
          color = Bench.Ink,
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
      HeroStat("POWER", formatPower(reading?.powerW), Bench.Mint, Modifier.weight(1f))
      HeroStat("TEMPERATURE", formatTemp(reading?.tempC), tempTone(reading?.tempC), Modifier.weight(1f))
      HeroStat(
        if (reading?.isPlugged == true) "TO FULL" else "TIME LEFT",
        if (reading?.isPlugged == true) {
          formatDuration(reading?.let { estimateToFull(it) })
        } else {
          formatDuration(reading?.let { estimateRemaining(samples, it, System.currentTimeMillis()) })
        },
        Bench.Lavender,
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
          color = Bench.Ink,
        )
        Text(
          reading?.let { plugLabel(it.plugged) + " · " + chargeSpeedLabel(it.currentMa, it.powerW, it.plugged) } ?: "",
          style = MaterialTheme.typography.bodySmall,
          color = Bench.InkMuted,
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
private fun BatteryGauge(
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
        color = Bench.Hairline,
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
              listOf(tone.copy(alpha = 0.55f), tone, if (charging) Bench.Lavender else tone),
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
          color = Bench.Ink,
        )
        Text(
          "%",
          style = MaterialTheme.typography.headlineMedium,
          color = Bench.InkMuted,
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
private fun HeroStat(
  label: String,
  value: String,
  tone: Color,
  modifier: Modifier = Modifier,
) {
  Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Text(label, style = AppType.micro, color = Bench.InkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Spacer(Modifier.height(5.dp))
    Text(value, style = AppType.dataSmall, color = tone, maxLines = 1, overflow = TextOverflow.Ellipsis)
  }
}

@Composable
private fun MetricGrid(
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
          tone = Bench.Mint,
          trend = samples.mapNotNull { it.currentMa?.toFloat() }.takeLast(30),
          footer = if (reading?.isCharging == true) "Going in" else "Draw",
        )
      }

      "voltage" -> {
        MetricTile(
          label = "Battery voltage",
          value = reading?.voltageMv?.let { "%.2f".format(it / 1000f) } ?: "—",
          unit = "V",
          tone = Bench.Sky,
          trend = samples.mapNotNull { it.voltageMv?.let { v -> v / 1000f } }.takeLast(30),
          footer = "Live reading",
        )
      }

      "power" -> {
        MetricTile(
          label = "Power",
          value = reading?.powerW?.let { "%.1f".format(it) } ?: "—",
          unit = "W",
          tone = Bench.Lavender,
          trend =
            samples
              .mapNotNull { p ->
                val ma = p.currentMa ?: return@mapNotNull null
                val mv = p.voltageMv ?: return@mapNotNull null
                (kotlin.math.abs(ma) * mv / 1_000_000f)
              }.takeLast(30),
          footer = reading?.let { chargeSpeedLabel(it.currentMa, it.powerW, it.plugged) },
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
private fun ChargingPanel(
  reading: BatteryReading?,
  sessions: List<BatterySession>,
) {
  val lastCharge = sessions.lastOrNull { it.charging }
  Panel {
    Eyebrow(label = "Charging") {
      StatusChip(
        reading?.let { if (it.isPlugged) chargeSpeedLabel(it.currentMa, it.powerW, it.plugged) else "Unplugged" } ?: "—",
        if (reading?.isPlugged == true) Bench.Lavender else Bench.InkFaint,
      )
    }
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.Bottom) {
      Text(
        reading?.takeIf { it.isPlugged }?.powerW?.let { "%.1f".format(it) } ?: "—",
        style = MaterialTheme.typography.displayMedium,
        color = Bench.Ink,
      )
      Spacer(Modifier.width(6.dp))
      Text("W", style = AppType.micro, color = Bench.InkFaint, modifier = Modifier.padding(bottom = 7.dp))
      Spacer(Modifier.weight(1f))
      Column(horizontalAlignment = Alignment.End) {
        Text(
          reading?.let { plugLabel(it.plugged) } ?: "—",
          style = AppType.mono,
          color = Bench.InkMuted,
        )
        Text(
          reading?.let { if (it.isPlugged) estimateToFull(it)?.let(::formatDuration) ?: "Measuring…" else "Not charging" } ?: "",
          style = AppType.micro,
          color = Bench.InkFaint,
        )
      }
    }
    Spacer(Modifier.height(10.dp))
    Meter(
      fraction = (reading?.level ?: 0) / 100f,
      tone = if (reading?.isPlugged == true) Bench.Mint else Bench.Lavender,
    )
    Spacer(Modifier.height(14.dp))
    if (lastCharge != null) {
      Text("LAST CHARGE", style = AppType.micro, color = Bench.InkFaint)
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
        color = Bench.InkMuted,
      )
    }
  }
}

@Composable
private fun TemperaturePanel(samples: List<BatterySample>) {
  val temps = samples.mapNotNull { it.tempC }
  Panel {
    Eyebrow(label = "Temperature · 24h") {
      if (temps.isNotEmpty()) {
        StatusChip("%.1f–%.1f\u00A0°C".format(temps.min(), temps.max()), tempTone(temps.max()))
      }
    }
    Spacer(Modifier.height(12.dp))
    if (temps.size < 2) {
      Text("Temperature history builds while the live monitor runs.", style = MaterialTheme.typography.bodySmall, color = Bench.InkMuted)
    } else {
      Sparkline(
        temps.takeLast(160),
        Bench.Sky,
        Modifier.fillMaxWidth().height(72.dp),
        fill = temps.max() - temps.min() > 0.2f,
        description = "Battery temperature trend, 24 hours",
      )
      Spacer(Modifier.height(9.dp))
      Row(Modifier.fillMaxWidth()) {
        Text("MIN %.1f\u00A0°C".format(temps.min()), style = AppType.micro, color = Bench.InkFaint)
        Spacer(Modifier.weight(1f))
        Text("MAX %.1f\u00A0°C".format(temps.max()), style = AppType.micro, color = Bench.InkFaint)
      }
    }
  }
}

@Composable
private fun AlarmPanel(
  settings: BatterySettings,
  update: ((BatterySettings) -> BatterySettings) -> Unit,
) {
  Panel {
    Eyebrow(label = "Alarms") {
      Text("EACH ONE IS OPTIONAL", style = AppType.micro, color = Bench.InkFaint)
    }
    Spacer(Modifier.height(10.dp))
    SwitchRow(
      title = "Full charge",
      blurb = "When the battery reaches 100%",
      icon = Icons.Outlined.BatteryFull,
      tone = Bench.Mint,
      checked = settings.notifyFull,
    ) { update { it.copy(notifyFull = !it.notifyFull) } }
    SwitchRow(
      title = "Low battery",
      blurb = "Alert at ${settings.lowLevel}%",
      icon = Icons.Outlined.Bolt,
      tone = Bench.Rose,
      checked = settings.notifyLow,
    ) { update { it.copy(notifyLow = !it.notifyLow) } }
    if (settings.notifyLow) {
      Stepper(settings.lowLevel, 2, 50, "%", "low battery level") { value -> update { it.copy(lowLevel = value) } }
    }
    SwitchRow(
      title = "Level alarm",
      blurb = "Reminder at a level you pick",
      icon = Icons.Outlined.NotificationsActive,
      tone = Bench.Amber,
      checked = settings.notifyLevel,
    ) { update { it.copy(notifyLevel = !it.notifyLevel) } }
    if (settings.notifyLevel) {
      Stepper(settings.levelAlarm, 5, 100, "%", "level alarm") { value -> update { it.copy(levelAlarm = value) } }
    }
    SwitchRow(
      title = "High temperature",
      blurb = "Above ${settings.highTempC}\u00A0°C",
      icon = Icons.Outlined.Thermostat,
      tone = Bench.Amber,
      checked = settings.notifyHighTemp,
    ) { update { it.copy(notifyHighTemp = !it.notifyHighTemp) } }
    if (settings.notifyHighTemp) {
      Stepper(settings.highTempC, 35, 70, "°C", "high temperature") { value -> update { it.copy(highTempC = value) } }
    }
    SwitchRow(
      title = "Low temperature",
      blurb = "Below ${settings.lowTempC}\u00A0°C",
      icon = Icons.Outlined.Thermostat,
      tone = Bench.Sky,
      checked = settings.notifyLowTemp,
    ) { update { it.copy(notifyLowTemp = !it.notifyLowTemp) } }
    if (settings.notifyLowTemp) {
      Stepper(settings.lowTempC, -20, 20, "°C", "low temperature") { value -> update { it.copy(lowTempC = value) } }
    }
  }
}

@Composable
private fun MonitorPanel(
  settings: BatterySettings,
  monitorStatus: MonitorStatus,
  update: ((BatterySettings) -> BatterySettings) -> Unit,
) {
  val context = LocalContext.current
  Panel {
    Eyebrow(label = "Live monitor") {
      MonitorChip(settings.monitorEnabled, monitorStatus)
    }
    Spacer(Modifier.height(12.dp))
    Text(
      "One silent notification shows the live percentage, power, temperature and time left. The status-bar icon can follow it.",
      style = MaterialTheme.typography.bodySmall,
      color = Bench.InkMuted,
    )
    Spacer(Modifier.height(14.dp))
    SwitchRow(
      title = "Charging summary",
      blurb = "A short report after you unplug",
      icon = Icons.Outlined.BatteryChargingFull,
      tone = Bench.Lavender,
      checked = settings.chargingSummary,
    ) { update { it.copy(chargingSummary = !it.chargingSummary) } }
    SwitchRow(
      title = "Overnight report",
      blurb = "Battery drop each morning",
      icon = Icons.Outlined.Timer,
      tone = Bench.Sky,
      checked = settings.overnightReport,
    ) { update { it.copy(overnightReport = !it.overnightReport) } }
    SwitchRow(
      title = "Weekly report",
      blurb = "Recap every Monday",
      icon = Icons.Outlined.Refresh,
      tone = Bench.Amber,
      checked = settings.weeklySummary,
    ) { update { it.copy(weeklySummary = !it.weeklySummary) } }
    SwitchRow(
      title = "Abnormal drain",
      blurb = "When screen-off drain is far above usual",
      icon = Icons.Outlined.Info,
      tone = Bench.Rose,
      checked = settings.anomalyAlert,
    ) { update { it.copy(anomalyAlert = !it.anomalyAlert) } }
    Spacer(Modifier.height(14.dp))
    Hairline()
    Spacer(Modifier.height(13.dp))
    Text("STATUS BAR ICON", style = AppType.micro, color = Bench.InkFaint)
    Spacer(Modifier.height(9.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
      IconChoice("Off", settings.iconStyle == BatterySettings.ICON_OFF, Modifier.weight(1f)) {
        update { it.copy(iconStyle = BatterySettings.ICON_OFF) }
      }
      IconChoice("Percent", settings.iconStyle == BatterySettings.ICON_PERCENT, Modifier.weight(1f)) {
        update { it.copy(iconStyle = BatterySettings.ICON_PERCENT) }
      }
      IconChoice("Percent + °", settings.iconStyle == BatterySettings.ICON_PERCENT_TEMP, Modifier.weight(1f)) {
        update { it.copy(iconStyle = BatterySettings.ICON_PERCENT_TEMP) }
      }
      IconChoice("Ring", settings.iconStyle == BatterySettings.ICON_RING, Modifier.weight(1f)) {
        update { it.copy(iconStyle = BatterySettings.ICON_RING) }
      }
    }
    Spacer(Modifier.height(9.dp))
    Text(
      "Some phones always draw the app icon in the status bar. That is controlled by the system.",
      style = MaterialTheme.typography.bodySmall,
      color = Bench.InkFaint,
    )
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      GhostButton("Notification settings", Icons.Outlined.Settings, onClick = {
        runCatching {
          context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
              .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
          )
        }
      }, modifier = Modifier.weight(1f))
      GhostButton("Battery settings", Icons.Outlined.Settings, onClick = {
        runCatching { context.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)) }
      }, modifier = Modifier.weight(1f))
    }
  }
}

@Composable
private fun MonitorChip(enabled: Boolean, status: MonitorStatus) {
  val label =
    when {
      !enabled -> R.string.tile_monitor_paused
      status == MonitorStatus.RUNNING -> R.string.battery_monitor_chip_running
      status == MonitorStatus.STARTING -> R.string.battery_monitor_chip_starting
      else -> R.string.tile_monitor_unavailable
    }
  StatusChip(stringResource(label), if (enabled && status == MonitorStatus.RUNNING) Bench.Mint else Bench.InkFaint)
}

@Composable
private fun Stepper(
  value: Int,
  min: Int,
  max: Int,
  unit: String,
  label: String,
  onChange: (Int) -> Unit,
) {
  Row(
    Modifier.fillMaxWidth().padding(start = 45.dp, bottom = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      if (unit == "%") "$value$unit" else "$value\u00A0$unit",
      style = AppType.monoStrong,
      color = Bench.Ink,
    )
    Spacer(Modifier.weight(1f))
    StepButton("−", "Decrease $label") { onChange((value - 1).coerceAtLeast(min)) }
    Spacer(Modifier.width(8.dp))
    StepButton("+", "Increase $label") { onChange((value + 1).coerceAtMost(max)) }
  }
}

@Composable
private fun StepButton(
  label: String,
  description: String,
  onClick: () -> Unit,
) {
  Box(
    Modifier
      .size(48.dp)
      .clip(RoundedCornerShape(9.dp))
      .background(Bench.Sunken)
      .border(1.dp, Bench.Hairline, RoundedCornerShape(9.dp))
      .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
      .semantics { contentDescription = description },
    contentAlignment = Alignment.Center,
  ) {
    Text(label, style = AppType.monoStrong, color = Bench.Ink)
  }
}

@Composable
private fun IconChoice(
  label: String,
  active: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  Box(
    modifier
      .heightIn(min = 48.dp)
      .clip(RoundedCornerShape(50))
      .background(if (active) Bench.Signal.copy(alpha = 0.14f) else Bench.Sunken)
      .border(1.dp, if (active) Bench.Signal.copy(alpha = 0.5f) else Bench.Hairline, RoundedCornerShape(50))
      .selectable(selected = active, role = androidx.compose.ui.semantics.Role.RadioButton, onClick = onClick)
      .padding(vertical = 8.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      label.uppercase(),
      style = AppType.micro.copy(letterSpacing = 0.2.sp),
      color = if (active) Bench.Signal else Bench.InkMuted,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun HealthPanel(
  health: com.c0d3h01.androidtweaker.data.HealthEstimate,
  reading: BatteryReading?,
  sessions: List<BatterySession>,
) {
  Panel(container = Bench.PanelRaised, borderColor = Bench.Lavender.copy(alpha = 0.22f)) {
    Eyebrow(label = "Battery health") {
      StatusChip(health.confidence, Bench.Lavender, dot = true)
    }
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.Bottom) {
      Text(
        health.percent?.toString() ?: "—",
        style = MaterialTheme.typography.displayLarge.copy(fontSize = 54.sp, lineHeight = 58.sp),
        color = if (health.percent == null) Bench.InkFaint else Bench.Ink,
      )
      if (health.percent != null) {
        Spacer(Modifier.width(4.dp))
        Text("%", style = MaterialTheme.typography.headlineSmall, color = Bench.InkMuted, modifier = Modifier.padding(bottom = 7.dp))
      }
      Spacer(Modifier.weight(1f))
      StatusChip(health.label, healthTone(health.percent))
    }
    Spacer(Modifier.height(8.dp))
    Text(
      "${formatMah(health.capacityMah)} of ${formatMah(health.designMah ?: reading?.designMah)} design capacity",
      style = MaterialTheme.typography.bodyMedium,
      color = Bench.InkMuted,
    )
    Spacer(Modifier.height(14.dp))
    Meter((health.percent ?: 0) / 100f, healthTone(health.percent))
    Spacer(Modifier.height(14.dp))
    InfoRow("Measured from", "${health.sessionsUsed} charge sessions")
    Spacer(Modifier.height(7.dp))
    InfoRow("Cycles", health.cycles?.toString() ?: "Not reported")
    Spacer(Modifier.height(7.dp))
    InfoRow("Design capacity", formatMah(reading?.designMah))
    Spacer(Modifier.height(7.dp))
    InfoRow("Phone reports", formatMah(reading?.fullMah))
    Spacer(Modifier.height(12.dp))
    Text(
      when (health.confidence) {
        "Verified" -> "Several clean charges agree closely, so this reading is reliable."
        "Estimated" -> "Enough charges measured, but they disagree. More charges from low will settle it."
        "Approximate" -> "A rough reading. Charge from below 30% to full a few times to improve it."
        else -> "No usable charge yet. Charge at least 15% with the screen off for the first reading."
      },
      style = MaterialTheme.typography.bodySmall,
      color = Bench.InkMuted,
    )
  }
}

@Composable
private fun HealthChart(
  sessions: List<BatterySession>,
  designMah: Int?,
) {
  val charges = sessions.filter { it.charging && it.capacityMah != null }.takeLast(16)
  Panel {
    Eyebrow(label = "Measured capacity") {
      Text("${charges.size} CHARGES", style = AppType.micro, color = Bench.InkFaint)
    }
    Spacer(Modifier.height(12.dp))
    if (charges.size < 2) {
      Text("2 measured charges are needed for the trend.", style = MaterialTheme.typography.bodySmall, color = Bench.InkMuted)
    } else {
      CapacityBars(charges.map { it.capacityMah!!.toFloat() }, designMah, Modifier.fillMaxWidth().height(110.dp))
      Spacer(Modifier.height(8.dp))
      Row(Modifier.fillMaxWidth()) {
        Text("OLDEST", style = AppType.micro, color = Bench.InkFaint)
        Spacer(Modifier.weight(1f))
        Text("LATEST", style = AppType.micro, color = Bench.InkFaint)
      }
    }
  }
}

@Composable
private fun CapacityBars(
  values: List<Float>,
  designMah: Int?,
  modifier: Modifier = Modifier,
) {
  Canvas(
    modifier.semantics {
      contentDescription =
        buildString {
          append("Capacity trend across ${values.size} charges")
          values.lastOrNull()?.let { append(", latest ${formatMah(it.toInt())}") }
          designMah?.let { append(", design ${formatMah(it)}") }
        }
    },
  ) {
    if (values.isEmpty()) return@Canvas
    val max = (designMah?.toFloat() ?: values.max()).coerceAtLeast(values.max())
    val gap = 3.dp.toPx()
    val width = (size.width - gap * (values.size - 1)) / values.size
    values.forEachIndexed { index, value ->
      val height = (value / max).coerceIn(0f, 1f) * size.height
      val x = index * (width + gap)
      drawRoundRect(
        color = if (index == values.lastIndex) Bench.Lavender else Bench.Lavender.copy(alpha = 0.38f),
        topLeft = Offset(x, size.height - height),
        size = Size(width, height),
        cornerRadius =
          androidx.compose.ui.geometry
            .CornerRadius(3.dp.toPx()),
      )
    }
    if (designMah != null) {
      drawLine(
        Bench.HairlineStrong,
        Offset(0f, size.height * (1f - designMah.toFloat() / max).coerceIn(0f, 1f)),
        Offset(size.width, size.height * (1f - designMah.toFloat() / max).coerceIn(0f, 1f)),
        strokeWidth = 1.dp.toPx(),
      )
    }
  }
}

@Composable
private fun ChargeSessions(sessions: List<BatterySession>) {
  val charges = sessions.filter { it.charging }.takeLast(8).reversed()
  Panel {
    Eyebrow(label = "Charge sessions") {
      Text("${sessions.count { it.charging }} TOTAL", style = AppType.micro, color = Bench.InkFaint)
    }
    Spacer(Modifier.height(10.dp))
    if (charges.isEmpty()) {
      Text("No charge sessions yet.", style = MaterialTheme.typography.bodySmall, color = Bench.InkMuted)
    } else {
      charges.forEachIndexed { index, session ->
        if (index > 0) {
          Spacer(Modifier.height(11.dp))
          Hairline()
          Spacer(Modifier.height(11.dp))
        }
        SessionRow(session)
      }
    }
  }
}

@Composable
private fun SessionRow(session: BatterySession) {
  val context = LocalContext.current
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Bench.Mint.copy(alpha = 0.09f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(Icons.Outlined.BatteryChargingFull, contentDescription = null, tint = Bench.Mint, modifier = Modifier.size(17.dp))
    }
    Spacer(Modifier.width(11.dp))
    Column(Modifier.weight(1f)) {
      Text(
        "${session.startLevel}% → ${session.endLevel}% · ${formatDuration(session.durationMs)}",
        style = AppType.monoStrong,
        color = Bench.Ink,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        "${formatMah(session.addedMah.let { if (it > 0) it else null })} added · ${formatPower(session.peakPowerW)} peak",
        style = MaterialTheme.typography.bodySmall,
        color = Bench.InkMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Spacer(Modifier.width(8.dp))
    Column(horizontalAlignment = Alignment.End) {
      Text("${session.quality}", style = AppType.monoStrong, color = qualityTone(session.quality))
      Text(dateClock(context, session.end), style = AppType.micro, color = Bench.InkFaint)
    }
  }
}

@Composable
private fun UsageOverview(
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
      StatusChip(if (stats.screenOnMs + stats.screenOffMs > 0) "Tracked" else "Collecting", Bench.Sky)
    }
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      StatBox("SCREEN ON", formatDuration(stats.screenOnMs), Bench.Mint, Modifier.weight(1f))
      StatBox("SCREEN OFF", formatDuration(stats.screenOffMs), Bench.InkMuted, Modifier.weight(1f))
    }
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      StatBox("DEEP SLEEP", formatDuration(stats.deepSleepMs), Bench.Lavender, Modifier.weight(1f))
      StatBox("AWAKE", formatDuration(stats.awakeMs), Bench.Amber, Modifier.weight(1f))
    }
    Spacer(Modifier.height(13.dp))
    Meter(
      fraction = if (total > 0) stats.screenOnMs.toFloat() / total else 0f,
      tone = Bench.Mint,
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
      color = Bench.InkMuted,
    )
    Spacer(Modifier.height(14.dp))
    Hairline()
    Spacer(Modifier.height(13.dp))
    InfoRow(
      "Drain · screen on",
      screenOnRate?.let { "%.1f%%/h".format(it) } ?: "Measuring…",
      Bench.Mint,
    )
    Spacer(Modifier.height(7.dp))
    InfoRow(
      "Drain · screen off",
      screenOffRate?.let { "%.1f%%/h".format(it) } ?: "Measuring…",
      Bench.Lavender,
    )
    Spacer(Modifier.height(7.dp))
    InfoRow(
      "Estimated left",
      formatDuration(reading?.let { estimateRemaining(samples, it, System.currentTimeMillis()) }),
      Bench.Sky,
    )
  }
}

@Composable
private fun StatBox(
  label: String,
  value: String,
  tone: Color,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier
      .clip(RoundedCornerShape(14.dp))
      .background(Bench.Sunken)
      .border(1.dp, Bench.Hairline, RoundedCornerShape(14.dp))
      .padding(horizontal = 12.dp, vertical = 11.dp),
  ) {
    Text(label, style = AppType.micro, color = Bench.InkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Spacer(Modifier.height(4.dp))
    Text(value, style = AppType.dataSmall, color = tone)
  }
}

@Composable
private fun AppUsagePanel(
  apps: List<AppUsage>,
  access: Boolean,
  onOpenSettings: () -> Unit,
) {
  Panel {
    Eyebrow(label = "App usage") {
      if (access) Text("SINCE UNPLUG", style = AppType.micro, color = Bench.InkFaint)
    }
    Spacer(Modifier.height(10.dp))
    if (!access) {
      Text(
        "Grant Usage access to see which apps used the screen and how much of the session each one took.",
        style = MaterialTheme.typography.bodySmall,
        color = Bench.InkMuted,
      )
      Spacer(Modifier.height(12.dp))
      GhostButton("Allow usage access", Icons.Outlined.Settings, onClick = onOpenSettings)
    } else if (apps.isEmpty()) {
      Text(
        "No app usage yet. Unplug and use the phone for a few minutes.",
        style = MaterialTheme.typography.bodySmall,
        color = Bench.InkMuted,
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
            Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(Bench.Signal.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              app.label.take(1).uppercase(),
              style = AppType.monoStrong,
              color = Bench.Signal,
            )
          }
          Spacer(Modifier.width(10.dp))
          Column(Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.titleMedium, color = Bench.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
              "${formatDuration(app.screenMs)} on screen",
              style = MaterialTheme.typography.bodySmall,
              color = Bench.InkMuted,
            )
          }
          Column(horizontalAlignment = Alignment.End) {
            Text("%.0f%%".format(app.share * 100f), style = AppType.monoStrong, color = Bench.Mint)
            Text("${app.launches} starts", style = AppType.micro, color = Bench.InkFaint)
          }
        }
        Spacer(Modifier.height(7.dp))
        Meter(app.share.coerceIn(0f, 1f), Bench.Mint, height = 3.dp)
      }
    }
  }
}

@Composable
private fun AnomalyPanel(
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
      tone = Bench.Rose,
      checked = settings.anomalyAlert,
    ) { update { it.copy(anomalyAlert = !it.anomalyAlert) } }
    Text(
      "Measured on this device from your own sessions. Requires the live monitor to be running.",
      style = MaterialTheme.typography.bodySmall,
      color = Bench.InkFaint,
    )
  }
}

@Composable
private fun TipPanel(
  title: String,
  tips: List<String>,
) {
  Panel {
    Eyebrow(label = title)
    Spacer(Modifier.height(10.dp))
    tips.forEachIndexed { index, tip ->
      Row(verticalAlignment = Alignment.Top) {
        Text("${index + 1}", style = AppType.micro, color = Bench.Signal, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.width(9.dp))
        Text(tip, style = MaterialTheme.typography.bodySmall, color = Bench.InkMuted)
      }
      if (index != tips.lastIndex) Spacer(Modifier.height(8.dp))
    }
  }
}

@Composable
private fun WeekPanel(
  summary: BatterySummary,
  sessions: List<BatterySession>,
) {
  val days =
    (6 downTo 0).map { offset ->
      val end = System.currentTimeMillis() - offset * 24 * 3_600_000L
      val start = end - 24 * 3_600_000L
      val label =
        LocalDate
          .now()
          .minusDays(offset.toLong())
          .dayOfWeek
          .getDisplayName(TextStyle.SHORT, Locale.getDefault())
      label to
        sessions
          .filter { !it.charging && it.end in start..end }
          .sumOf { (it.startLevel - it.endLevel).coerceAtLeast(0) }
    }
  val peak = days.maxOfOrNull { it.second } ?: 0
  Panel {
    Eyebrow(label = "Last 7 days") {
      StatusChip("${summary.drainPercent}% drained", Bench.Lavender)
    }
    Spacer(Modifier.height(12.dp))
    Row(
      horizontalArrangement = Arrangement.spacedBy(5.dp),
      modifier =
        Modifier
          .fillMaxWidth()
          .height(104.dp)
          .clearAndSetSemantics {
            contentDescription =
              "Last 7 days: " + days.joinToString(", ") { "${it.first} ${it.second} percent" }
          },
    ) {
      days.forEach { (label, value) ->
        Column(
          Modifier.weight(1f).fillMaxHeight(),
          verticalArrangement = Arrangement.Bottom,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
            Box(
              Modifier
                .fillMaxWidth()
                .fillMaxHeight((value / 100f).coerceIn(0.03f, 1f))
                .clip(RoundedCornerShape(4.dp))
                .background(if (value == peak && value > 0) Bench.Signal else Bench.Signal.copy(alpha = 0.35f)),
            )
          }
          Spacer(Modifier.height(4.dp))
          Text(
            "$value%",
            style = AppType.micro.copy(letterSpacing = 0.sp),
            color = Bench.InkFaint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            label,
            style = AppType.micro.copy(letterSpacing = 0.sp),
            color = Bench.InkFaint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
    Spacer(Modifier.height(14.dp))
    Hairline()
    Spacer(Modifier.height(12.dp))
    InfoRow("Charges", summary.chargeCount.toString())
    Spacer(Modifier.height(7.dp))
    InfoRow("Added", formatMah(summary.addedMah.let { if (it > 0) it else null }))
    Spacer(Modifier.height(7.dp))
    InfoRow(
      "Charge quality",
      summary.averageQuality?.let { "$it · ${qualityTier(it)}" } ?: "—",
      summary.averageQuality?.let { qualityTone(it) } ?: Bench.InkMuted,
    )
    Spacer(Modifier.height(7.dp))
    InfoRow("Screen on", formatDuration(summary.screenOnMs))
    Spacer(Modifier.height(7.dp))
    InfoRow("Deep sleep", formatDuration(summary.deepSleepMs))
  }
}

@Composable
private fun SessionHistory(sessions: List<BatterySession>) {
  val context = LocalContext.current
  val recent = sessions.takeLast(20).reversed()
  Panel {
    Eyebrow(label = "Session history") {
      Text("${sessions.size} SAVED", style = AppType.micro, color = Bench.InkFaint)
    }
    Spacer(Modifier.height(10.dp))
    if (recent.isEmpty()) {
      Text(
        "Sessions appear after the monitor sees a charge or a discharge.",
        style = MaterialTheme.typography.bodySmall,
        color = Bench.InkMuted,
      )
    } else {
      recent.forEachIndexed { index, session ->
        if (index > 0) {
          Spacer(Modifier.height(11.dp))
          Hairline()
          Spacer(Modifier.height(11.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            Modifier
              .size(32.dp)
              .clip(RoundedCornerShape(10.dp))
              .background((if (session.charging) Bench.Mint else Bench.Lavender).copy(alpha = 0.09f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              if (session.charging) Icons.Outlined.BatteryChargingFull else Icons.Outlined.BatteryFull,
              contentDescription = null,
              tint = if (session.charging) Bench.Mint else Bench.Lavender,
              modifier = Modifier.size(16.dp),
            )
          }
          Spacer(Modifier.width(10.dp))
          Column(Modifier.weight(1f)) {
            Text(
              "${session.startLevel}% → ${session.endLevel}% · ${formatDuration(session.durationMs)}",
              style = AppType.monoStrong,
              color = Bench.Ink,
            )
            Text(
              if (session.charging) {
                "${formatMah(session.addedMah.let { if (it > 0) it else null })} added · quality ${session.quality}"
              } else {
                "Screen on ${formatDuration(session.screenOnMs)} · sleep ${formatDuration(session.deepSleepMs)}"
              },
              style = MaterialTheme.typography.bodySmall,
              color = Bench.InkMuted,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Text(dateClock(context, session.start), style = AppType.micro, color = Bench.InkFaint)
        }
      }
    }
  }
}

@Composable
private fun DataPanel(
  onCsv: () -> Unit,
  onBackup: () -> Unit,
  onDelete: () -> Unit,
) {
  Panel {
    Eyebrow(label = "Data") {
      Text("LOCAL ONLY", style = AppType.micro, color = Bench.InkFaint)
    }
    Spacer(Modifier.height(10.dp))
    GhostButton("Export CSV", Icons.Outlined.Download, onClick = onCsv)
    Spacer(Modifier.height(8.dp))
    GhostButton("Export backup", Icons.Outlined.Share, onClick = onBackup)
    Spacer(Modifier.height(10.dp))
    GhostButton("Delete history", Icons.Outlined.DeleteOutline, onClick = onDelete)
  }
}

@Composable
private fun InfoNote(text: String) {
  Row(
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(Bench.Sunken)
      .border(1.dp, Bench.Hairline, RoundedCornerShape(14.dp))
      .padding(13.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Icon(Icons.Outlined.Info, contentDescription = null, tint = Bench.InkFaint, modifier = Modifier.size(16.dp))
    Spacer(Modifier.width(9.dp))
    Text(text, style = MaterialTheme.typography.bodySmall, color = Bench.InkMuted)
  }
}

private fun openUsageAccess(context: android.content.Context) {
  val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
  runCatching { context.startActivity(intent) }
    .onFailure { Toast.makeText(context, "Open Settings → Usage access", Toast.LENGTH_SHORT).show() }
}

private fun statusLabel(
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

private fun tempTone(c: Float?): Color =
  when {
    c == null -> Bench.InkMuted
    c >= 60f -> Bench.Rose
    c >= 45f -> Bench.Amber
    c <= 5f -> Bench.Sky
    else -> Bench.Mint
  }

private fun tempLabel(c: Float): String =
  when {
    c >= 60f -> "Very hot"
    c >= 45f -> "Warm"
    c <= 5f -> "Cold"
    else -> "Normal"
  }

private fun healthTone(percent: Int?): Color =
  when {
    percent == null -> Bench.InkFaint
    percent >= 85 -> Bench.Mint
    percent >= 70 -> Bench.Amber
    else -> Bench.Rose
  }

private fun qualityTone(score: Int): Color =
  when {
    score >= 80 -> Bench.Mint
    score >= 55 -> Bench.Amber
    else -> Bench.Rose
  }
