package com.c0d3h01.androidtweaker.battery.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.c0d3h01.androidtweaker.BatteryMonitorService.MonitorStatus
import com.c0d3h01.androidtweaker.R
import com.c0d3h01.androidtweaker.battery.BatterySettings
import com.c0d3h01.androidtweaker.ui.components.Eyebrow
import com.c0d3h01.androidtweaker.ui.components.GhostButton
import com.c0d3h01.androidtweaker.ui.components.Hairline
import com.c0d3h01.androidtweaker.ui.components.Panel
import com.c0d3h01.androidtweaker.ui.components.StatusChip
import com.c0d3h01.androidtweaker.ui.components.SwitchRow
import com.c0d3h01.androidtweaker.ui.theme.AppColors
import com.c0d3h01.androidtweaker.ui.theme.AppType

@Composable
internal fun AlarmPanel(
  settings: BatterySettings,
  update: ((BatterySettings) -> BatterySettings) -> Unit,
) {
  Panel {
    Eyebrow(label = "Alarms") {
      Text("EACH ONE IS OPTIONAL", style = AppType.micro, color = AppColors.InkFaint)
    }
    Spacer(Modifier.height(10.dp))
    SwitchRow(
      title = "Full charge",
      blurb = "When the battery reaches 100%",
      icon = Icons.Outlined.BatteryFull,
      tone = AppColors.Mint,
      checked = settings.notifyFull,
    ) { update { it.copy(notifyFull = !it.notifyFull) } }
    SwitchRow(
      title = "Low battery",
      blurb = "Alert at ${settings.lowLevel}%",
      icon = Icons.Outlined.Bolt,
      tone = AppColors.Rose,
      checked = settings.notifyLow,
    ) { update { it.copy(notifyLow = !it.notifyLow) } }
    if (settings.notifyLow) {
      Stepper(settings.lowLevel, 2, 50, "%", "low battery level") { value -> update { it.copy(lowLevel = value) } }
    }
    SwitchRow(
      title = "Level alarm",
      blurb = "Reminder at a level you pick",
      icon = Icons.Outlined.NotificationsActive,
      tone = AppColors.Amber,
      checked = settings.notifyLevel,
    ) { update { it.copy(notifyLevel = !it.notifyLevel) } }
    if (settings.notifyLevel) {
      Stepper(settings.levelAlarm, 5, 100, "%", "level alarm") { value -> update { it.copy(levelAlarm = value) } }
    }
    SwitchRow(
      title = "High temperature",
      blurb = "Above ${settings.highTempC}\u00A0°C",
      icon = Icons.Outlined.Thermostat,
      tone = AppColors.Amber,
      checked = settings.notifyHighTemp,
    ) { update { it.copy(notifyHighTemp = !it.notifyHighTemp) } }
    if (settings.notifyHighTemp) {
      Stepper(settings.highTempC, 35, 70, "°C", "high temperature") { value -> update { it.copy(highTempC = value) } }
    }
    SwitchRow(
      title = "Low temperature",
      blurb = "Below ${settings.lowTempC}\u00A0°C",
      icon = Icons.Outlined.Thermostat,
      tone = AppColors.Sky,
      checked = settings.notifyLowTemp,
    ) { update { it.copy(notifyLowTemp = !it.notifyLowTemp) } }
    if (settings.notifyLowTemp) {
      Stepper(settings.lowTempC, -20, 20, "°C", "low temperature") { value -> update { it.copy(lowTempC = value) } }
    }
  }
}

@Composable
internal fun MonitorPanel(
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
      color = AppColors.InkMuted,
    )
    Spacer(Modifier.height(14.dp))
    SwitchRow(
      title = "Charging summary",
      blurb = "A short report after you unplug",
      icon = Icons.Outlined.BatteryChargingFull,
      tone = AppColors.Lavender,
      checked = settings.chargingSummary,
    ) { update { it.copy(chargingSummary = !it.chargingSummary) } }
    SwitchRow(
      title = "Overnight report",
      blurb = "Battery drop each morning",
      icon = Icons.Outlined.Timer,
      tone = AppColors.Sky,
      checked = settings.overnightReport,
    ) { update { it.copy(overnightReport = !it.overnightReport) } }
    SwitchRow(
      title = "Weekly report",
      blurb = "Recap every Monday",
      icon = Icons.Outlined.Refresh,
      tone = AppColors.Amber,
      checked = settings.weeklySummary,
    ) { update { it.copy(weeklySummary = !it.weeklySummary) } }
    SwitchRow(
      title = "Abnormal drain",
      blurb = "When screen-off drain is far above usual",
      icon = Icons.Outlined.Info,
      tone = AppColors.Rose,
      checked = settings.anomalyAlert,
    ) { update { it.copy(anomalyAlert = !it.anomalyAlert) } }
    Spacer(Modifier.height(14.dp))
    Hairline()
    Spacer(Modifier.height(13.dp))
    Text("STATUS BAR ICON", style = AppType.micro, color = AppColors.InkFaint)
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
      color = AppColors.InkFaint,
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
internal fun MonitorChip(enabled: Boolean, status: MonitorStatus) {
  val label =
    when {
      !enabled -> R.string.tile_monitor_paused
      status == MonitorStatus.RUNNING -> R.string.battery_monitor_chip_running
      status == MonitorStatus.STARTING -> R.string.battery_monitor_chip_starting
      else -> R.string.tile_monitor_unavailable
    }
  StatusChip(stringResource(label), if (enabled && status == MonitorStatus.RUNNING) AppColors.Mint else AppColors.InkFaint)
}

@Composable
internal fun Stepper(
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
      color = AppColors.Ink,
    )
    Spacer(Modifier.weight(1f))
    StepButton("−", "Decrease $label") { onChange((value - 1).coerceAtLeast(min)) }
    Spacer(Modifier.width(8.dp))
    StepButton("+", "Increase $label") { onChange((value + 1).coerceAtMost(max)) }
  }
}

@Composable
internal fun StepButton(
  label: String,
  description: String,
  onClick: () -> Unit,
) {
  Box(
    Modifier
      .size(48.dp)
      .clip(RoundedCornerShape(9.dp))
      .background(AppColors.Sunken)
      .border(1.dp, AppColors.Hairline, RoundedCornerShape(9.dp))
      .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
      .semantics { contentDescription = description },
    contentAlignment = Alignment.Center,
  ) {
    Text(label, style = AppType.monoStrong, color = AppColors.Ink)
  }
}

@Composable
internal fun IconChoice(
  label: String,
  active: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  Box(
    modifier
      .heightIn(min = 48.dp)
      .clip(RoundedCornerShape(50))
      .background(if (active) AppColors.Signal.copy(alpha = 0.14f) else AppColors.Sunken)
      .border(1.dp, if (active) AppColors.Signal.copy(alpha = 0.5f) else AppColors.Hairline, RoundedCornerShape(50))
      .selectable(selected = active, role = androidx.compose.ui.semantics.Role.RadioButton, onClick = onClick)
      .padding(vertical = 8.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      label.uppercase(),
      style = AppType.micro.copy(letterSpacing = 0.2.sp),
      color = if (active) AppColors.Signal else AppColors.InkMuted,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}
