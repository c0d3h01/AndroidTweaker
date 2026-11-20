package com.c0d3h01.androidtweaker.tuning

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.c0d3h01.androidtweaker.ui.components.ChoiceChipRow
import com.c0d3h01.androidtweaker.ui.components.Eyebrow
import com.c0d3h01.androidtweaker.ui.components.Hairline
import com.c0d3h01.androidtweaker.ui.components.Panel
import com.c0d3h01.androidtweaker.ui.theme.AppColors

internal fun LazyListScope.displayControls(features: TuningSettings, onFeatures: (TuningSettings) -> Unit) {
  item {
    Eyebrow(label = "Device controls")
  }
  item {
    Panel {
      Text("Resolution", style = MaterialTheme.typography.titleMedium, color = AppColors.Ink)
      Spacer(Modifier.height(8.dp))
      val resolutions = Resolution.entries
      ChoiceChipRow(
        labels = resolutions.map { it.label },
        selected = resolutions.indexOf(features.resolution),
        onSelect = { index -> onFeatures(features.copy(resolution = resolutions[index])) },
      )
      Hairline(Modifier.padding(vertical = 10.dp))
      Text("Refresh-rate cap", style = MaterialTheme.typography.titleMedium, color = AppColors.Ink)
      Spacer(Modifier.height(8.dp))
      val refreshCaps = listOf(0, 60, 90, 120)
      ChoiceChipRow(
        labels = refreshCaps.map { if (it == 0) "Off" else "$it\u00A0Hz" },
        selected = refreshCaps.indexOf(features.peakFps),
        onSelect = { index -> onFeatures(features.copy(peakFps = refreshCaps[index])) },
      )
    }
  }

}
