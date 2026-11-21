package com.c0d3h01.androidtweaker.tuning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.c0d3h01.androidtweaker.access.AccessManager
import com.c0d3h01.androidtweaker.ui.components.Eyebrow
import com.c0d3h01.androidtweaker.ui.components.Hairline
import com.c0d3h01.androidtweaker.ui.components.SwitchRow

internal fun LazyListScope.networkControls(features: TuningSettings, onFeatures: (TuningSettings) -> Unit, access: AccessManager.Mode, busy: Boolean) {
  item {
    Eyebrow(label = "Network")
  }
  item {
    TuningPanel(TuningFeature.NETWORK, access, busy) {
      SwitchRow(
        title = "Keep Wi-Fi awake",
        blurb = "Disable Wi-Fi sleep while the device is plugged in.",
        checked = features.wifiSleep,
        onChange = { onFeatures(features.copy(wifiSleep = it)) },
      )
      Hairline(Modifier.padding(vertical = 2.dp))
      SwitchRow(
        title = "Always-on scanning",
        blurb = "Keep Wi-Fi and Bluetooth scanning available in the background.",
        checked = features.scanAlways,
        onChange = { onFeatures(features.copy(scanAlways = it)) },
      )
    }
  }

}
