package com.c0d3h01.androidtweaker.tuning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.c0d3h01.androidtweaker.ui.components.Eyebrow
import com.c0d3h01.androidtweaker.ui.components.Hairline
import com.c0d3h01.androidtweaker.ui.components.Panel
import com.c0d3h01.androidtweaker.ui.components.SwitchRow

internal fun LazyListScope.powerControls(
  features: TuningSettings,
  onFeatures: (TuningSettings) -> Unit,
  onPickApps: (AppPickerKind) -> Unit,
) {
  item {
    Eyebrow(label = "Doze & battery")
  }
  item {
    Panel {
      SwitchRow(
        title = "Force doze",
        blurb = "Enter device idle as soon as the screen turns off.",
        checked = features.forceDoze,
        onChange = { onFeatures(features.copy(forceDoze = it)) },
      )
      Hairline(Modifier.padding(vertical = 2.dp))
      SwitchRow(
        title = "Kill background apps",
        blurb = "Close background processes on every screen-off.",
        checked = features.killBackground,
        onChange = { onFeatures(features.copy(killBackground = it)) },
      )
      Hairline(Modifier.padding(vertical = 2.dp))
      SwitchRow(
        title = "Auto power save",
        blurb = "Turn on the system battery saver while the screen is off.",
        checked = features.autoPowerSave,
        onChange = { onFeatures(features.copy(autoPowerSave = it)) },
      )
      Hairline(Modifier.padding(vertical = 2.dp))
      SwitchRow(
        title = "Auto data saver",
        blurb = "Restrict background data while the screen is off.",
        checked = features.autoDataSaver,
        onChange = { onFeatures(features.copy(autoDataSaver = it)) },
      )
      Hairline(Modifier.padding(vertical = 2.dp))
      PickerRow(
        label = "Doze whitelist",
        value = "${features.dozeWhitelist.size} apps",
        onClick = { onPickApps(AppPickerKind.WHITELIST) },
      )
    }
  }

}
