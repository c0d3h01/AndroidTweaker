package com.c0d3h01.androidtweaker.device

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.c0d3h01.androidtweaker.R
import com.c0d3h01.androidtweaker.access.AccessManager
import com.c0d3h01.androidtweaker.access.RootInfo
import com.c0d3h01.androidtweaker.access.titleRes
import com.c0d3h01.androidtweaker.profiles.PROFILE_SPECS
import com.c0d3h01.androidtweaker.profiles.Profile
import com.c0d3h01.androidtweaker.ui.components.EmptyState
import com.c0d3h01.androidtweaker.ui.components.Eyebrow
import com.c0d3h01.androidtweaker.ui.components.GhostButton
import com.c0d3h01.androidtweaker.ui.components.Panel
import com.c0d3h01.androidtweaker.ui.components.ScreenTitle
import com.c0d3h01.androidtweaker.ui.components.StatusChip
import com.c0d3h01.androidtweaker.ui.components.Wordmark
import com.c0d3h01.androidtweaker.ui.theme.AppColors
import com.c0d3h01.androidtweaker.ui.theme.AppType

@Composable
fun PerformanceScreen(
  snapshot: TelemetrySnapshot?,
  root: RootInfo?,
  currentCode: Int,
  pendingCode: Int?,
  loading: Boolean,
  cpuHistory: List<Float>,
  onRefresh: () -> Unit,
  onSelect: (Profile) -> Unit,
  onOpenHardware: () -> Unit,
  onOpenTweaks: () -> Unit,
  onOpenAccess: () -> Unit,
  shell: AccessManager.Mode,
  quickBusy: Boolean,
  onQuickCommand: (String) -> Unit,
) {
  val current = Profile.fromCode(currentCode)
  val activeSpec = PROFILE_SPECS.firstOrNull { it.profile == current }

  LazyColumn(
    Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = AppColors.Gutter, end = AppColors.Gutter, top = 12.dp, bottom = AppColors.BarClearance),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item { Wordmark(onRefresh = onRefresh) }
    item {
      ScreenTitle(title = stringResource(R.string.nav_performance), subtitle = "Profile & live state") {
        StatusChip(
          stringResource(shell.titleRes),
          if (shell.canApplyProfiles) AppColors.Mint else AppColors.InkMuted,
        )
      }
    }
    if (!shell.canApplyProfiles) {
      item {
        EmptyState(
          title = stringResource(R.string.tuning_apply_root_required),
          body = stringResource(R.string.profile_access_required),
          action = { GhostButton(stringResource(R.string.tuning_access_settings), onClick = onOpenAccess) },
        )
      }
    }

    item { SystemStrip(snapshot = snapshot, root = root) }

    item {
      ActiveProfilePanel(
        code = currentCode,
        spec = activeSpec,
        pending = pendingCode != null,
        governor =
          snapshot
            ?.cpu
            ?.clusters
            ?.firstOrNull()
            ?.governor,
        device = snapshot?.device?.model,
      )
    }

    item {
      Eyebrow(label = "Quick actions") {
        if (quickBusy) StatusChip("Running…", AppColors.Signal)
      }
    }
    item {
      Panel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          GhostButton(
            "Free RAM",
            onClick = { onQuickCommand("clean ram") },
            modifier = Modifier.weight(1f),
            enabled = !quickBusy && shell.canApplyProfiles,
          )
          GhostButton(
            "Clear cache",
            onClick = { onQuickCommand("clean cache") },
            modifier = Modifier.weight(1f),
            enabled = !quickBusy && shell.canApplyProfiles,
          )
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          GhostButton(
            "Force doze",
            onClick = { onQuickCommand("doze force") },
            modifier = Modifier.weight(1f),
            enabled = !quickBusy && shell != AccessManager.Mode.NONE,
          )
          GhostButton(
            "Apply features",
            onClick = { onQuickCommand("feature") },
            modifier = Modifier.weight(1f),
            enabled = !quickBusy && shell != AccessManager.Mode.NONE,
          )
        }
      }
    }

    item {
      Eyebrow(label = "Live snapshot") {
        Row(
          Modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onOpenHardware)
            .padding(horizontal = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text("OPEN HARDWARE", style = AppType.micro, color = AppColors.Signal)
          Text("→", style = AppType.micro, color = AppColors.Signal)
        }
      }
    }
    item {
      TelemetryGrid(snapshot, cpuHistory, onOpenHardware)
    }

    item {
      Eyebrow(label = "Profiles") {
        Text(
          if (loading) "READING…" else if (shell.canApplyProfiles) "TAP TO APPLY" else "ROOT REQUIRED",
          style = AppType.micro,
          color = AppColors.InkFaint,
        )
      }
    }
    items(PROFILE_SPECS.size) { index ->
      val spec = PROFILE_SPECS[index]
      ProfileRow(
        spec = spec,
        selected = currentCode == spec.profile.code,
        pending = pendingCode == spec.profile.code,
        onSelect = { onSelect(spec.profile) },
        enabled = shell.canApplyProfiles && !quickBusy,
      )
    }

    item { CustomizeEntry(onOpenTweaks) }

    item { SystemDetailsPanel(snapshot, root) }

    item {
      Text(
        "PROFILE CHANGES APPLY WITHOUT A REBOOT  ·  NO MODULE REQUIRED",
        style = AppType.micro.copy(letterSpacing = 0.2.sp),
        color = AppColors.InkFaint,
        modifier = Modifier.padding(top = 2.dp),
      )
    }
  }
}
