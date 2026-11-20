package com.c0d3h01.androidtweaker.battery

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.c0d3h01.androidtweaker.BatteryMonitorService
import com.c0d3h01.androidtweaker.battery.analysis.estimateHealth
import com.c0d3h01.androidtweaker.battery.ui.AlarmPanel
import com.c0d3h01.androidtweaker.battery.ui.AnomalyPanel
import com.c0d3h01.androidtweaker.battery.ui.AppUsagePanel
import com.c0d3h01.androidtweaker.battery.ui.ChargeSessions
import com.c0d3h01.androidtweaker.battery.ui.ChargingPanel
import com.c0d3h01.androidtweaker.battery.ui.DataPanel
import com.c0d3h01.androidtweaker.battery.ui.HealthChart
import com.c0d3h01.androidtweaker.battery.ui.HealthPanel
import com.c0d3h01.androidtweaker.battery.ui.HeroPanel
import com.c0d3h01.androidtweaker.battery.ui.InfoNote
import com.c0d3h01.androidtweaker.battery.ui.MetricGrid
import com.c0d3h01.androidtweaker.battery.ui.MonitorPanel
import com.c0d3h01.androidtweaker.battery.ui.SessionHistory
import com.c0d3h01.androidtweaker.battery.ui.TemperaturePanel
import com.c0d3h01.androidtweaker.battery.ui.TipPanel
import com.c0d3h01.androidtweaker.battery.ui.UsageOverview
import com.c0d3h01.androidtweaker.battery.ui.WeekPanel
import com.c0d3h01.androidtweaker.battery.ui.openUsageAccess
import com.c0d3h01.androidtweaker.ui.components.ScreenTitle
import com.c0d3h01.androidtweaker.ui.components.StatusChip
import com.c0d3h01.androidtweaker.ui.components.TabStrip
import com.c0d3h01.androidtweaker.ui.components.Wordmark
import com.c0d3h01.androidtweaker.ui.theme.AppColors
import java.io.File

private val TABS = listOf("Overview", "Health", "Usage", "History")

@Composable
fun BatteryScreen(
  onRequestNotifications: () -> Unit = {},
  model: BatteryViewModel,
) {
  val context = LocalContext.current
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  val monitorStatus by BatteryMonitorService.status.collectAsStateWithLifecycle()
  val state by model.state.collectAsStateWithLifecycle()
  val reading = state.reading
  val settings = state.settings
  val sessions = state.sessions
  val samples = state.samples
  val apps = state.apps
  val usageAccess = state.usageAccess
  var tab by rememberSaveable { mutableIntStateOf(0) }
  var confirmDelete by remember { mutableStateOf(false) }

  LaunchedEffect(model, lifecycle) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { model.refreshWhileVisible() }
  }
  LaunchedEffect(tab, sessions.size, usageAccess) {
    if (tab == 2 && usageAccess) model.refreshUsage()
  }

  fun update(change: (BatterySettings) -> BatterySettings) = model.updateSettings(change)

  fun setMonitor(on: Boolean) {
    if (on) onRequestNotifications()
    model.setMonitoring(on)
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
  val week = remember(sessions) { BatteryRepository.summarySince(context, System.currentTimeMillis() - 7 * 24 * 3_600_000L) }
  val statusTone =
    when {
      reading == null -> AppColors.InkFaint
      (reading?.level ?: 100) <= 15 -> AppColors.Rose
      reading?.isCharging == true -> AppColors.Mint
      else -> AppColors.Lavender
    }

  LazyColumn(
    Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = AppColors.Gutter, end = AppColors.Gutter, top = 12.dp, bottom = AppColors.BarClearance),
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
    item { TabStrip(labels = TABS, selected = tab, onSelect = { tab = it }) }

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
            onCsv = { share(BatteryExport.exportCsv(context), "text/csv", "Battery CSV") },
            onBackup = { share(BatteryExport.backupJson(context), "application/json", "Battery backup") },
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
      containerColor = AppColors.Panel,
      titleContentColor = AppColors.Ink,
      textContentColor = AppColors.InkMuted,
      shape = RoundedCornerShape(20.dp),
      title = { Text("Delete battery history?") },
      text = { Text("This deletes all samples and sessions. Settings and alarms stay. You can't undo it.") },
      confirmButton = {
        TextButton(onClick = {
          model.deleteHistory()
          confirmDelete = false
        }) {
          Text("Delete", color = AppColors.Rose)
        }
      },
      dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = AppColors.InkMuted) } },
    )
  }
}
