package com.c0d3h01.androidtweaker.charging

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.c0d3h01.androidtweaker.R
import androidx.compose.ui.unit.dp
import com.c0d3h01.androidtweaker.access.AccessManager
import com.c0d3h01.androidtweaker.tuning.TuningFeature
import com.c0d3h01.androidtweaker.tuning.TuningPanel
import com.c0d3h01.androidtweaker.tuning.TuningSettings
import com.c0d3h01.androidtweaker.ui.components.ChoiceChipRow
import com.c0d3h01.androidtweaker.ui.components.Eyebrow
import com.c0d3h01.androidtweaker.ui.components.Hairline
import com.c0d3h01.androidtweaker.ui.components.SwitchRow
import com.c0d3h01.androidtweaker.ui.theme.AppColors

internal fun LazyListScope.chargingControls(
  features: TuningSettings,
  onFeatures: (TuningSettings) -> Unit,
  access: AccessManager.Mode,
  busy: Boolean,
) {
  val rooted = access.canApplyProfiles
  item {
    Eyebrow(label = "Health & charging")
  }
  item {
    TuningPanel(TuningFeature.CHARGING, access, busy) {
      Text("Charge limit", style = MaterialTheme.typography.titleMedium, color = AppColors.Ink)
      Spacer(Modifier.height(2.dp))
      Text(
        stringResource(R.string.charging_limit_detail),
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.InkFaint,
      )
      Spacer(Modifier.height(10.dp))
      val chargeLevels = listOf(0, 60, 70, 80, 90, 100)
      ChoiceChipRow(
        labels = chargeLevels.map { if (it == 0) "Off" else "$it%" },
        selected = chargeLevels.indexOf(features.charging.chargeLimit),
        onSelect = { index -> if (rooted) onFeatures(features.copy(charging = features.charging.copy(chargeLimit = chargeLevels[index]))) },
      )
      Spacer(Modifier.height(4.dp))
      SwitchRow(
        title = "Charging bypass",
        blurb = "Hold charging while the battery monitor runs.",
        checked = features.charging.chargeBypass,
        enabled = rooted,
        onChange = { onFeatures(features.copy(charging = features.charging.copy(chargeBypass = it))) },
      )
      Hairline(Modifier.padding(vertical = 2.dp))
      Text("Temperature fuse", style = MaterialTheme.typography.titleMedium, color = AppColors.Ink)
      Spacer(Modifier.height(2.dp))
      Text(
        "Holds charging when the pack gets this hot while the battery monitor runs.",
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.InkFaint,
      )
      Spacer(Modifier.height(10.dp))
      val fuseTemps = listOf(0, 40, 45, 50, 55)
      ChoiceChipRow(
        labels = fuseTemps.map { if (it == 0) "Off" else "$it\u00A0°C" },
        selected = fuseTemps.indexOf(features.charging.tempFuse),
        onSelect = { index -> if (rooted) onFeatures(features.copy(charging = features.charging.copy(tempFuse = fuseTemps[index]))) },
      )
    }
  }

}
