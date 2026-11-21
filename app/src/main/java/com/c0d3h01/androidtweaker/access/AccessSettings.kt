package com.c0d3h01.androidtweaker.access

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.c0d3h01.androidtweaker.ui.components.Eyebrow
import com.c0d3h01.androidtweaker.ui.components.GhostButton
import com.c0d3h01.androidtweaker.ui.components.InfoRow
import com.c0d3h01.androidtweaker.ui.components.Panel
import com.c0d3h01.androidtweaker.ui.theme.AppColors

internal fun LazyListScope.accessSettings(shell: AccessManager.Mode, onGrantShizuku: () -> Unit, onOpenShizuku: () -> Unit) {
  item {
    Eyebrow(label = "Shell access")
  }
  item {
    Panel {
      when (shell) {
        AccessManager.Mode.ROOT, AccessManager.Mode.SHIZUKU_ROOT -> {
          InfoRow("Backend", stringResource(shell.titleRes))
          Spacer(Modifier.height(6.dp))
          Text(
            "Kernel tuning is available on devices that expose writable settings.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.InkFaint,
          )
        }

        AccessManager.Mode.SHIZUKU -> {
          InfoRow("Backend", "Shizuku · non-root", AppColors.Sky)
          Spacer(Modifier.height(6.dp))
          Text(
            "Settings, cleanup, doze, network and app profiles run as shell. Sysfs groups stay off.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.InkFaint,
          )
        }

        AccessManager.Mode.NONE -> {
          InfoRow("Backend", "None", AppColors.Rose)
          Spacer(Modifier.height(6.dp))
          Text(
            "Root unlocks every group. Without root, install Shizuku and grant this app access to run the non-root features.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.InkFaint,
          )
          Spacer(Modifier.height(10.dp))
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton(
              "Grant Shizuku",
              onClick = onGrantShizuku,
              modifier = Modifier.weight(1f),
            )
            GhostButton(
              "Open Shizuku",
              onClick = onOpenShizuku,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }
  }}
