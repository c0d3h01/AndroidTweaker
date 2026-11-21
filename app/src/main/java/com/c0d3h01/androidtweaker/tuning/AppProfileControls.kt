package com.c0d3h01.androidtweaker.tuning

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.c0d3h01.androidtweaker.ui.components.Eyebrow
import com.c0d3h01.androidtweaker.ui.components.Hairline
import com.c0d3h01.androidtweaker.ui.components.Panel
import com.c0d3h01.androidtweaker.ui.theme.AppColors

internal fun LazyListScope.appProfileControls(features: TuningSettings, onPickApps: (AppPickerKind) -> Unit) {
  item {
    Eyebrow(label = "App profiles")
  }
  item {
    Panel {
      PickerRow(
        label = "Heavy apps",
        value = "${features.heavyApps.size} apps",
        onClick = { onPickApps(AppPickerKind.HEAVY) },
      )
      Text(
        "Performance game mode and RT priority while they run.",
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.InkFaint,
      )
      Hairline(Modifier.padding(vertical = 10.dp))
      PickerRow(
        label = "Light apps",
        value = "${features.lightApps.size} apps",
        onClick = { onPickApps(AppPickerKind.LIGHT) },
      )
      Text(
        "Battery game mode for apps that do not need full speed.",
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.InkFaint,
      )
    }
  }

}
