package com.c0d3h01.androidtweaker.tuning

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.c0d3h01.androidtweaker.R
import com.c0d3h01.androidtweaker.access.AccessManager
import com.c0d3h01.androidtweaker.ui.components.ChoiceChipRow
import com.c0d3h01.androidtweaker.ui.components.Eyebrow
import com.c0d3h01.androidtweaker.ui.components.GhostButton
import com.c0d3h01.androidtweaker.ui.components.Hairline
import com.c0d3h01.androidtweaker.ui.components.LocalControlsEnabled
import com.c0d3h01.androidtweaker.ui.components.SwitchRow
import com.c0d3h01.androidtweaker.ui.theme.AppColors
import com.c0d3h01.androidtweaker.ui.theme.AppType

internal fun LazyListScope.systemControls(
  features: TuningSettings,
  onFeatures: (TuningSettings) -> Unit,
  access: AccessManager.Mode,
  busy: Boolean,
  onCommand: (String) -> Unit,
  onPickApps: (AppPickerKind) -> Unit,
  onRemoveBloat: () -> Unit,
  onRestore: (String) -> Unit,
) {
  val rooted = access.canApplyProfiles
  item {
    Eyebrow(label = "Cleanup & system rules")
  }
  item {
    TuningPanel(TuningFeature.MAINTENANCE, access, busy) {
      val controlsEnabled = LocalControlsEnabled.current
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GhostButton("Free RAM", onClick = { onCommand("clean ram") }, modifier = Modifier.weight(1f), enabled = rooted)
        GhostButton("Clear cache", onClick = { onCommand("clean cache") }, modifier = Modifier.weight(1f), enabled = rooted)
      }
      Spacer(Modifier.height(8.dp))
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GhostButton("Clear logs", onClick = { onCommand("clean logs") }, modifier = Modifier.weight(1f))
        GhostButton("Recompile ART", onClick = { onCommand("clean art") }, modifier = Modifier.weight(1f))
      }
      Spacer(Modifier.height(6.dp))
      Hairline(Modifier.padding(vertical = 2.dp))
      SwitchRow(
        title = "Reduce debug logging",
        blurb = "Cuts log buffers and verbose kernel/ART tracing.",
        checked = features.disableLogs,
        enabled = rooted,
        badge = if (rooted) null else stringResource(R.string.tuning_root_badge),
        onChange = { onFeatures(features.copy(disableLogs = it)) },
      )
      Hairline(Modifier.padding(vertical = 2.dp))
      Text("Phantom process limit", style = MaterialTheme.typography.titleMedium, color = AppColors.Ink)
      Spacer(Modifier.height(2.dp))
      Text(
        "Caps how many child processes an app may fork in the background.",
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.InkFaint,
      )
      Spacer(Modifier.height(10.dp))
      val phantomLimits = listOf(0, 8, 16, 32, 64)
      ChoiceChipRow(
        labels = phantomLimits.map { if (it == 0) "Off" else "$it" },
        selected = phantomLimits.indexOf(features.phantomLimit),
        onSelect = { index -> onFeatures(features.copy(phantomLimit = phantomLimits[index])) },
      )
      Hairline(Modifier.padding(vertical = 10.dp))
      PickerRow(
        label = "Bloatware",
        value = "${features.bloat.size} selected",
        onClick = { onPickApps(AppPickerKind.BLOAT) },
      )
      if (features.bloat.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        Spacer(Modifier.height(2.dp))
        features.bloat.sorted().take(6).forEach { pkg ->
          Row(
            Modifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              pkg,
              style = AppType.mono,
              color = AppColors.InkMuted,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            Box(
              Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clickable(
                  enabled = controlsEnabled,
                  role = androidx.compose.ui.semantics.Role.Button,
                  onClickLabel = "Restore $pkg",
                ) { onRestore(pkg) },
              contentAlignment = Alignment.Center,
            ) {
              Text(
                "RESTORE",
                style = AppType.micro,
                color = AppColors.Signal,
              )
            }
          }
        }
        if (features.bloat.size > 6) {
          Text("+${features.bloat.size - 6} more", style = AppType.micro, color = AppColors.InkFaint)
        }
        Spacer(Modifier.height(8.dp))
        GhostButton("Remove selected apps", onClick = onRemoveBloat)
      }
    }
  }

}
