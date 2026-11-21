package com.c0d3h01.androidtweaker.tuning

import androidx.compose.foundation.lazy.LazyListScope
import com.c0d3h01.androidtweaker.access.AccessManager
import com.c0d3h01.androidtweaker.access.accessSettings
import com.c0d3h01.androidtweaker.charging.chargingControls

/** Builds the global setting sections inside the Tuning page. */
fun LazyListScope.featureSections(
  features: TuningSettings,
  shell: AccessManager.Mode,
  onFeatures: (TuningSettings) -> Unit,
  onCommand: (String) -> Unit,
  onPickApps: (AppPickerKind) -> Unit,
  onRemoveBloat: () -> Unit,
  onRestore: (String) -> Unit,
  onGrantShizuku: () -> Unit,
  onOpenShizuku: () -> Unit,
  busy: Boolean,
) {

  chargingControls(features, onFeatures, shell, busy)
  thermalControls(features, onFeatures, shell, busy)
  powerControls(features, onFeatures, onPickApps, shell, busy)
  systemControls(features, onFeatures, shell, busy, onCommand, onPickApps, onRemoveBloat, onRestore)
  displayControls(features, onFeatures, shell, busy)
  networkControls(features, onFeatures, shell, busy)
  appProfileControls(features, onPickApps, shell, busy)
  accessSettings(shell, onGrantShizuku, onOpenShizuku)
}
