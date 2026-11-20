package com.c0d3h01.androidtweaker.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.c0d3h01.androidtweaker.R
import com.c0d3h01.androidtweaker.access.AccessManager
import com.c0d3h01.androidtweaker.access.titleRes
import com.c0d3h01.androidtweaker.app.AppState
import com.c0d3h01.androidtweaker.ui.components.GhostButton
import com.c0d3h01.androidtweaker.ui.components.Hairline
import com.c0d3h01.androidtweaker.ui.components.Panel
import com.c0d3h01.androidtweaker.ui.components.StatusChip
import com.c0d3h01.androidtweaker.ui.components.SwitchRow
import com.c0d3h01.androidtweaker.ui.theme.AppColors

@Composable
fun SettingsScreen(
  state: AppState,
  onConnectRoot: () -> Unit,
  onConnectShizuku: () -> Unit,
  onBootReapply: (Boolean) -> Unit,
  onDiagnostics: () -> Unit,
  onLogs: () -> Unit,
  onBattery: () -> Unit,
) {
  LazyColumn(
    Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = AppColors.Gutter, vertical = 20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Text(
        stringResource(R.string.nav_settings),
        style = MaterialTheme.typography.headlineLarge,
        color = AppColors.Ink,
        modifier = Modifier.semantics { heading() },
      )
    }
    item {
      Panel {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          SettingsHeading(R.string.settings_privileges)
          StatusChip(
            stringResource(state.access.titleRes),
            tone = if (state.access == AccessManager.Mode.NONE) AppColors.InkMuted else AppColors.Signal,
          )
          Text(
            stringResource(R.string.settings_access_detail),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.InkMuted,
          )
          if (state.access != AccessManager.Mode.ROOT) {
            GhostButton(
              stringResource(if (state.connecting) R.string.settings_connecting else R.string.settings_connect_root),
              onClick = onConnectRoot,
              enabled = !state.connecting,
            )
          }
          if (state.access == AccessManager.Mode.NONE) {
            GhostButton(stringResource(R.string.settings_connect_shizuku), onClick = onConnectShizuku)
          }
        }
      }
    }
    item {
      Panel {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          SettingsHeading(R.string.settings_background)
          val canReapply = state.profileAccess == AccessManager.Mode.ROOT
          SwitchRow(
            title = stringResource(R.string.settings_boot_reapply),
            blurb = stringResource(if (canReapply) R.string.settings_boot_reapply_detail else R.string.settings_boot_reapply_locked),
            checked = state.reapplyOnBoot && canReapply,
            enabled = canReapply,
            onChange = onBootReapply,
          )
          Hairline()
          GhostButton(stringResource(R.string.settings_battery_controls), Icons.Outlined.BatteryChargingFull, onBattery)
        }
      }
    }
    item {
      Panel {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          SettingsHeading(R.string.nav_diagnostics)
          Text(
            stringResource(R.string.settings_data_detail),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.InkMuted,
          )
          GhostButton(stringResource(R.string.nav_diagnostics), Icons.Outlined.Build, onDiagnostics)
          GhostButton(stringResource(R.string.nav_logs), Icons.Outlined.Terminal, onLogs)
        }
      }
    }
  }
}

@Composable
private fun SettingsHeading(title: Int) {
  Text(
    stringResource(title),
    style = MaterialTheme.typography.titleLarge,
    color = AppColors.Ink,
    modifier = Modifier.semantics { heading() },
  )
}
