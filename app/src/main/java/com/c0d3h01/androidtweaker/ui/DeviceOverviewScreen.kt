package com.c0d3h01.androidtweaker.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.PowerManager
import android.provider.Settings
import android.text.format.Formatter
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.c0d3h01.androidtweaker.R
import com.c0d3h01.androidtweaker.data.DeviceCapacity
import com.c0d3h01.androidtweaker.data.DeviceInternet
import com.c0d3h01.androidtweaker.data.DeviceOverview
import com.c0d3h01.androidtweaker.data.DeviceTransport
import com.c0d3h01.androidtweaker.data.readDeviceOverview
import com.c0d3h01.androidtweaker.util.clockWithSeconds
import com.c0d3h01.androidtweaker.util.dateTime
import java.text.NumberFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DeviceOverviewScreen(onOpenHardware: () -> Unit) {
  val context = LocalContext.current
  val view = LocalView.current
  var refresh by remember { mutableIntStateOf(0) }
  var overview by remember { mutableStateOf<DeviceOverview?>(null) }
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
  LaunchedEffect(context, view, refresh) {
    if (refresh > 0) {
      val display = view.display
      overview = withContext(Dispatchers.IO) { readDeviceOverview(context.applicationContext, display) }
    }
  }

  val sample = overview
  val sections = sample?.sections(context).orEmpty()
  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      Modifier.widthIn(max = 680.dp).fillMaxSize(),
      contentPadding = PaddingValues(start = Bench.Gutter, end = Bench.Gutter, top = 12.dp, bottom = Bench.BarClearance),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Column {
          Text(
            stringResource(R.string.device_overview_title),
            style = MaterialTheme.typography.headlineLarge,
            color = Bench.Ink,
            modifier = Modifier.semantics { heading() },
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              if (sample == null) stringResource(R.string.device_overview_loading)
              else stringResource(R.string.device_overview_updated, clockWithSeconds(context, sample.capturedAt)),
              style = MaterialTheme.typography.bodySmall,
              color = Bench.InkMuted,
              modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
            )
            if (sample != null) {
              IconSquare(Icons.Outlined.Share, stringResource(R.string.device_overview_share), onClick = {
                shareOverview(context, sample, sections)
              })
            }
            IconSquare(Icons.Outlined.Refresh, stringResource(R.string.device_overview_refresh), onClick = { refresh++ })
          }
        }
      }
      items(sections, key = { it.id }) { section -> OverviewPanel(section) }
      item {
        OutlinedButton(onClick = onOpenHardware, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
          Text(stringResource(R.string.device_overview_hardware), textAlign = TextAlign.Center)
        }
      }
    }
  }
}

private data class OverviewSection(
  @StringRes val id: Int,
  val title: String,
  val rows: List<Pair<Int, String>>,
  val settings: Pair<Int, String>,
)

@Composable
private fun OverviewPanel(section: OverviewSection) {
  val context = LocalContext.current
  Panel {
    Text(
      section.title,
      style = MaterialTheme.typography.titleLarge,
      color = Bench.Ink,
      modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      section.rows.forEach { (label, value) ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            stringResource(label),
            modifier = Modifier.weight(1f),
            color = Bench.InkMuted,
            style = MaterialTheme.typography.bodyMedium,
          )
          Text(
            value,
            modifier = Modifier.weight(1f),
            color = Bench.Ink,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
          )
        }
      }
    }
    val (label, action) = section.settings
    Spacer(Modifier.height(8.dp))
    TextButton(
      onClick = { openOverviewActivity(context, Intent(action), R.string.device_overview_settings_unavailable) },
      modifier = Modifier.heightIn(min = 48.dp),
    ) { Text(stringResource(label)) }
  }
}

private fun DeviceOverview.sections(context: Context): List<OverviewSection> = with(context) {
  val unavailable = getString(R.string.device_overview_unavailable)
  val locale = resources.configuration.locales[0]
  fun DeviceCapacity?.format(): String = this?.let {
    getString(
      R.string.device_overview_capacity_available,
      Formatter.formatShortFileSize(context, available),
      Formatter.formatShortFileSize(context, total),
    )
  } ?: unavailable

  fun Boolean?.format(): String = when (this) {
    true -> getString(R.string.device_overview_on)
    false -> getString(R.string.device_overview_off)
    null -> unavailable
  }

  val android = androidVersion?.let { getString(R.string.device_overview_android_version, it, apiLevel) }
    ?: getString(R.string.device_overview_android_api, apiLevel)
  val batteryLevel = batteryPercent?.let { NumberFormat.getPercentInstance(locale).format(it / 100.0) } ?: unavailable
  val charging = getString(when (batteryStatus) {
    BatteryManager.BATTERY_STATUS_CHARGING -> R.string.device_overview_charging
    BatteryManager.BATTERY_STATUS_DISCHARGING -> R.string.device_overview_discharging
    BatteryManager.BATTERY_STATUS_FULL -> R.string.device_overview_full
    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> R.string.device_overview_charge_idle
    else -> R.string.device_overview_unavailable
  })
  val thermal = getString(when (thermalStatus) {
    PowerManager.THERMAL_STATUS_NONE -> R.string.device_overview_thermal_none
    PowerManager.THERMAL_STATUS_LIGHT -> R.string.device_overview_thermal_light
    PowerManager.THERMAL_STATUS_MODERATE -> R.string.device_overview_thermal_moderate
    PowerManager.THERMAL_STATUS_SEVERE -> R.string.device_overview_thermal_severe
    PowerManager.THERMAL_STATUS_CRITICAL -> R.string.device_overview_thermal_critical
    PowerManager.THERMAL_STATUS_EMERGENCY -> R.string.device_overview_thermal_emergency
    PowerManager.THERMAL_STATUS_SHUTDOWN -> R.string.device_overview_thermal_shutdown
    else -> R.string.device_overview_unavailable
  })
  val resolution = displayMode?.let {
    getString(R.string.device_overview_resolution_value, it.physicalWidth, it.physicalHeight)
  } ?: unavailable
  val refreshRate = displayMode?.refreshRate?.takeIf { it.isFinite() && it > 0f }
    ?.let { getString(R.string.device_overview_refresh_rate_value, it) } ?: unavailable
  val internet = getString(when (network?.internet) {
    DeviceInternet.DISCONNECTED -> R.string.device_overview_internet_disconnected
    DeviceInternet.CAPTIVE_PORTAL -> R.string.device_overview_internet_captive
    DeviceInternet.VALIDATED -> R.string.device_overview_internet_validated
    DeviceInternet.UNVALIDATED -> R.string.device_overview_internet_unvalidated
    DeviceInternet.LOCAL_ONLY -> R.string.device_overview_internet_local
    null -> R.string.device_overview_unavailable
  })
  val transport = when (network?.transport) {
    DeviceTransport.VPN -> R.string.device_overview_transport_vpn
    DeviceTransport.WI_FI -> R.string.device_overview_transport_wifi
    DeviceTransport.CELLULAR -> R.string.device_overview_transport_cellular
    DeviceTransport.ETHERNET -> R.string.device_overview_transport_ethernet
    DeviceTransport.BLUETOOTH -> R.string.device_overview_transport_bluetooth
    DeviceTransport.OTHER -> R.string.device_overview_transport_other
    null -> null
  }

  listOf(
    OverviewSection(
      R.string.device_overview_device,
      model ?: getString(R.string.device_overview_device),
      listOf(
        R.string.device_overview_manufacturer to (manufacturer ?: unavailable),
        R.string.device_overview_android to android,
        R.string.device_overview_security_patch to (securityPatch ?: unavailable),
      ),
      R.string.device_overview_about_settings to Settings.ACTION_DEVICE_INFO_SETTINGS,
    ),
    OverviewSection(
      R.string.device_overview_capacity,
      getString(R.string.device_overview_capacity),
      listOf(R.string.device_overview_storage to storage.format(), R.string.device_overview_memory to memory.format()),
      R.string.device_overview_storage_settings to Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
    ),
    OverviewSection(
      R.string.device_overview_power,
      getString(R.string.device_overview_power),
      listOf(
        R.string.device_overview_battery to batteryLevel,
        R.string.device_overview_battery_status to charging,
        R.string.device_overview_battery_saver to powerSaver.format(),
        R.string.device_overview_thermal to thermal,
      ),
      R.string.device_overview_battery_settings to Settings.ACTION_BATTERY_SAVER_SETTINGS,
    ),
    OverviewSection(
      R.string.device_overview_display,
      getString(R.string.device_overview_display),
      listOf(R.string.device_overview_resolution to resolution, R.string.device_overview_refresh_rate to refreshRate),
      R.string.device_overview_display_settings to Settings.ACTION_DISPLAY_SETTINGS,
    ),
    OverviewSection(
      R.string.device_overview_network,
      getString(R.string.device_overview_network),
      buildList {
        add(R.string.device_overview_internet to internet)
        transport?.let { add(R.string.device_overview_transport to getString(it)) }
        network?.metered?.let {
          val usage = if (it) R.string.device_overview_metered else R.string.device_overview_unmetered
          add(R.string.device_overview_data_usage to getString(usage))
        }
      },
      R.string.device_overview_network_settings to Settings.ACTION_WIRELESS_SETTINGS,
    ),
  )
}

private fun shareOverview(context: Context, overview: DeviceOverview, sections: List<OverviewSection>) {
  val title = context.getString(R.string.device_overview_share_title)
  val report = buildString {
    appendLine(title)
    appendLine(context.getString(R.string.device_overview_updated, dateTime(context, overview.capturedAt)))
    sections.forEach { section ->
      appendLine()
      appendLine(section.title)
      section.rows.forEach { (label, value) ->
        appendLine(context.getString(R.string.device_overview_share_line, context.getString(label), value))
      }
    }
  }.trimEnd()
  val intent = Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_SUBJECT, title)
    putExtra(Intent.EXTRA_TEXT, report)
  }
  openOverviewActivity(context, Intent.createChooser(intent, null), R.string.device_overview_share_unavailable)
}

private fun openOverviewActivity(context: Context, intent: Intent, @StringRes error: Int) {
  try {
    context.startActivity(intent)
  } catch (_: ActivityNotFoundException) {
    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
  } catch (_: SecurityException) {
    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
  }
}
