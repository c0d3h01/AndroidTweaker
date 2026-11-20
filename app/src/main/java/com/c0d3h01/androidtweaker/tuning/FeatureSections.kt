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
) {
  val rooted = shell.canApplyProfiles

  chargingControls(features, onFeatures, rooted)
  thermalControls(features, onFeatures, rooted)
  powerControls(features, onFeatures, onPickApps)
  systemControls(features, onFeatures, rooted, onCommand, onPickApps, onRemoveBloat, onRestore)
  displayControls(features, onFeatures)
  networkControls(features, onFeatures)
  appProfileControls(features, onPickApps)
  accessSettings(shell, onGrantShizuku, onOpenShizuku)
}
