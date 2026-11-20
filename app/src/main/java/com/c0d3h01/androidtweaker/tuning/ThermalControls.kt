package com.c0d3h01.androidtweaker.tuning

import androidx.compose.foundation.lazy.LazyListScope
import com.c0d3h01.androidtweaker.ui.components.Eyebrow
import com.c0d3h01.androidtweaker.ui.components.Panel
import com.c0d3h01.androidtweaker.ui.components.StatusChip
import com.c0d3h01.androidtweaker.ui.components.SwitchRow
import com.c0d3h01.androidtweaker.ui.theme.AppColors

internal fun LazyListScope.thermalControls(
  features: TuningSettings,
  onFeatures: (TuningSettings) -> Unit,
  rooted: Boolean,
) {
  item {
    Eyebrow(label = "Thermal") {
      if (!rooted) StatusChip("Root", AppColors.Amber, dot = false)
    }
  }
  item {
    Panel {
      SwitchRow(
        title = "Disable throttling",
        blurb = "Stops thermal services and disables trip points. Heat damage is on you; keep an eye on battery temperature.",
        checked = features.throttleOff,
        enabled = rooted,
        onChange = { onFeatures(features.copy(throttleOff = it)) },
      )
    }
  }

}
