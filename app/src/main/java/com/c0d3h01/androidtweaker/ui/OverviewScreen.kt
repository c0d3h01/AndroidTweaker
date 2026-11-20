package com.c0d3h01.androidtweaker.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.c0d3h01.androidtweaker.BatteryMonitorService
import com.c0d3h01.androidtweaker.R
import com.c0d3h01.androidtweaker.TweakerState
import com.c0d3h01.androidtweaker.data.BatteryStore
import com.c0d3h01.androidtweaker.data.Profile
import com.c0d3h01.androidtweaker.util.ShellAccess

@Composable
fun OverviewScreen(
  state: TweakerState,
  onBattery: () -> Unit,
  onPerformance: () -> Unit,
) {
  val context = LocalContext.current
  val power = remember(context) { context.getSystemService(PowerManager::class.java) }
  var level by remember { mutableStateOf<Int?>(null) }
  var status by remember { mutableStateOf(BatteryManager.BATTERY_STATUS_UNKNOWN) }
  var powerSave by remember { mutableStateOf(false) }
  val monitor by BatteryMonitorService.status.collectAsStateWithLifecycle()
  LifecycleStartEffect(context) {
    fun update(intent: Intent) {
      if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
        val raw = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0)
        level = if (raw in 0..scale && scale > 0) ((raw.toLong() * 100 + scale / 2) / scale).toInt() else null
        status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
      }
      powerSave = power?.isPowerSaveMode == true
    }
    val receiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) = update(intent)
    }
    val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED).apply {
      addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
    }
    ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)?.let(::update)
    powerSave = power?.isPowerSaveMode == true
    onStopOrDispose { context.unregisterReceiver(receiver) }
  }

  val monitorText = when (monitor) {
    BatteryMonitorService.MonitorStatus.STOPPED -> if (BatteryStore.settings(context).monitorEnabled) {
      R.string.battery_monitor_unavailable
    } else {
      R.string.battery_monitor_stopped
    }
    BatteryMonitorService.MonitorStatus.STARTING -> R.string.battery_monitor_starting
    BatteryMonitorService.MonitorStatus.RUNNING -> R.string.battery_monitor_running
    BatteryMonitorService.MonitorStatus.UNAVAILABLE -> R.string.battery_monitor_unavailable
  }
  val batteryState = when (status) {
    BatteryManager.BATTERY_STATUS_CHARGING -> R.string.status_charging
    BatteryManager.BATTERY_STATUS_FULL -> R.string.battery_state_full
    BatteryManager.BATTERY_STATUS_DISCHARGING -> R.string.status_on_battery
    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> R.string.battery_state_idle
    else -> R.string.reading_unavailable
  }
  val profile = Profile.fromCode(state.profileCode)
  val profileName = PROFILE_SPECS.firstOrNull { it.profile == profile }?.title

  LazyColumn(
    Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = Bench.Gutter, vertical = 20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          stringResource(R.string.app_name),
          style = MaterialTheme.typography.headlineLarge,
          color = Bench.Ink,
          modifier = Modifier.semantics { heading() },
        )
        Text(
          stringResource(R.string.overview_device, Build.MODEL, Build.VERSION.RELEASE),
          style = MaterialTheme.typography.bodyMedium,
          color = Bench.InkMuted,
        )
      }
    }

    item {
      Panel(container = Bench.SignalSoft, borderColor = Bench.Signal.copy(alpha = 0.30f)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
              Icons.Outlined.BatteryChargingFull,
              contentDescription = null,
              modifier = Modifier.size(26.dp),
              tint = Bench.Signal,
            )
            Column(Modifier.weight(1f)) {
              Text(stringResource(R.string.nav_battery), style = MaterialTheme.typography.titleLarge, color = Bench.Ink)
              Text(stringResource(batteryState), style = MaterialTheme.typography.bodyMedium, color = Bench.InkMuted)
            }
            Text(
              level?.let { stringResource(R.string.overview_percent, it) } ?: stringResource(R.string.reading_unavailable),
              style = MaterialTheme.typography.headlineMedium,
              color = Bench.Signal,
            )
          }
          level?.let { current ->
            LinearProgressIndicator(
              progress = { current / 100f },
              modifier = Modifier.fillMaxWidth(),
              color = Bench.Signal,
              trackColor = Bench.HairlineStrong,
            )
          }
          Text(stringResource(monitorText), style = MaterialTheme.typography.bodyMedium, color = Bench.InkMuted)
          GhostButton(stringResource(R.string.overview_battery_action), onClick = onBattery)
        }
      }
    }

    item {
      Panel {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            stringResource(R.string.overview_status),
            style = MaterialTheme.typography.titleLarge,
            color = Bench.Ink,
            modifier = Modifier.semantics { heading() },
          )
          InfoRow(stringResource(R.string.access_title), stringResource(state.access.titleRes))
          InfoRow(
            stringResource(R.string.overview_power_saver),
            stringResource(if (powerSave) R.string.setting_on else R.string.setting_off),
          )
          InfoRow(
            stringResource(R.string.overview_last_profile),
            profileName?.let { stringResource(it) } ?: stringResource(R.string.overview_no_profile),
          )
          Hairline()
          GhostButton(stringResource(R.string.nav_performance), Icons.Outlined.Speed, onPerformance)
        }
      }
    }
  }
}

internal val ShellAccess.Mode.titleRes: Int
  get() = when (this) {
    ShellAccess.Mode.NONE -> R.string.access_stock
    ShellAccess.Mode.ROOT -> R.string.access_root
    ShellAccess.Mode.SHIZUKU -> R.string.access_shizuku
    ShellAccess.Mode.SHIZUKU_ROOT -> R.string.access_shizuku_root
  }
