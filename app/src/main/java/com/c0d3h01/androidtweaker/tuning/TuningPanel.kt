package com.c0d3h01.androidtweaker.tuning

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.c0d3h01.androidtweaker.R
import com.c0d3h01.androidtweaker.access.AccessManager
import com.c0d3h01.androidtweaker.ui.components.Panel
import com.c0d3h01.androidtweaker.ui.theme.AppColors

/** One access-aware container for every tuning group. */
@Composable
internal fun TuningPanel(
  feature: TuningFeature,
  access: AccessManager.Mode,
  busy: Boolean,
  content: @Composable ColumnScope.() -> Unit,
) {
  val available = feature.available(access)
  Column {
    if (!available) {
      Text(
        stringResource(if (feature.needsRoot) R.string.tuning_needs_root else R.string.tuning_needs_shell),
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.Amber,
      )
      Spacer(Modifier.height(8.dp))
    }
    Panel(enabled = available && !busy, content = content)
  }
}
