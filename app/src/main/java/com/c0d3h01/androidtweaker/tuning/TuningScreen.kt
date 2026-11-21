package com.c0d3h01.androidtweaker.tuning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.c0d3h01.androidtweaker.R
import com.c0d3h01.androidtweaker.access.AccessManager
import com.c0d3h01.androidtweaker.profiles.PROFILE_SPECS
import com.c0d3h01.androidtweaker.profiles.Profile
import com.c0d3h01.androidtweaker.profiles.ProfileSettings
import com.c0d3h01.androidtweaker.profiles.ProfileSettingsStore
import com.c0d3h01.androidtweaker.profiles.profileControls
import com.c0d3h01.androidtweaker.ui.components.GhostButton
import com.c0d3h01.androidtweaker.ui.components.Panel
import com.c0d3h01.androidtweaker.ui.components.ScreenTitle
import com.c0d3h01.androidtweaker.ui.components.StatusChip
import com.c0d3h01.androidtweaker.ui.components.TabStrip
import com.c0d3h01.androidtweaker.ui.components.Wordmark
import com.c0d3h01.androidtweaker.ui.theme.AppColors

/**
 * Per-profile customization: pick the engine groups each mode applies, then
 * override the limits those groups would otherwise take from the profile.
 * Everything saves immediately; the apply button writes the conf the engine
 * sources, so a later boot reapply uses the same settings.
 */
@Composable
fun TuningScreen(
  currentCode: Int,
  pendingCode: Int?,
  features: TuningSettings,
  shell: AccessManager.Mode,
  onApply: (Profile) -> Unit,
  onFeatures: (TuningSettings) -> Unit,
  onCommand: (String) -> Unit,
  onGrantShizuku: () -> Unit,
  onOpenShizuku: () -> Unit,
  onOpenAccess: () -> Unit,
  busy: Boolean,
) {
  val context = LocalContext.current
  var tab by rememberSaveable { mutableIntStateOf(0) }
  var picker by remember { mutableStateOf<AppPickerKind?>(null) }
  var confirmBloat by remember { mutableStateOf(false) }
  var index by rememberSaveable {
    mutableIntStateOf(Profile.fromCode(currentCode)?.ordinal ?: Profile.BALANCED.ordinal)
  }
  val selectedIndex = index.coerceIn(Profile.entries.indices)
  val profile = Profile.entries[selectedIndex]
  val spec = PROFILE_SPECS.firstOrNull { it.profile == profile }
  val tone = spec?.tone ?: AppColors.Signal
  val profileLabels = PROFILE_SPECS.map { stringResource(it.title) }
  var prefs by remember { mutableStateOf(ProfileSettings()) }

  fun reload() {
    prefs = ProfileSettingsStore.load(context, profile)
  }

  fun update(next: ProfileSettings) {
    if (!TuningFeature.PROFILES.available(AccessManager.mode()) || busy) return
    prefs = next.sanitized()
    ProfileSettingsStore.save(context, profile, prefs)
  }

  LaunchedEffect(profile) { reload() }

  LazyColumn(
    Modifier.fillMaxSize(),
    contentPadding =
      PaddingValues(start = AppColors.Gutter, end = AppColors.Gutter, top = 12.dp, bottom = AppColors.BarClearance),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item { Wordmark(onRefresh = { reload() }) }
    item {
      ScreenTitle(title = "Tweaks", subtitle = "Modes & feature switches") {
        if (currentCode == profile.code) StatusChip(stringResource(R.string.tuning_saved_profile), tone)
      }
    }

    if (!shell.canApplyProfiles) {
      item {
        Panel {
          Text(
            stringResource(R.string.profile_access_required),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.InkMuted,
          )
          Spacer(Modifier.height(8.dp))
          GhostButton(stringResource(R.string.tuning_access_settings), onClick = onOpenAccess)
        }
      }
    }

    item {
      TabStrip(
        labels = listOf("Modes", "Features"),
        selected = tab,
        onSelect = { tab = it },
      )
    }

    if (tab == 0) {
      profileControls(
        profile, spec, prefs, profileLabels, selectedIndex, { index = it },
        pendingCode, shell, busy, onApply, ::update,
      )
    } else {
      featureSections(
        features = features,
        shell = shell,
        onFeatures = onFeatures,
        onCommand = onCommand,
        onPickApps = { picker = it },
        onRemoveBloat = { confirmBloat = true },
        onRestore = { pkg -> onCommand("restore $pkg") },
        onGrantShizuku = onGrantShizuku,
        onOpenShizuku = onOpenShizuku,
        busy = busy,
      )
    }
  }

  picker?.let { kind ->
    val selected =
      when (kind) {
        AppPickerKind.HEAVY -> features.heavyApps
        AppPickerKind.LIGHT -> features.lightApps
        AppPickerKind.WHITELIST -> features.dozeWhitelist
        AppPickerKind.BLOAT -> features.bloat
      }
    AppPickerDialog(
      kind = kind,
      selected = selected,
      onDismiss = { picker = null },
      onSave = { picked ->
        val next =
          when (kind) {
            AppPickerKind.HEAVY -> features.copy(heavyApps = picked)
            AppPickerKind.LIGHT -> features.copy(lightApps = picked)
            AppPickerKind.WHITELIST -> features.copy(dozeWhitelist = picked)
            AppPickerKind.BLOAT -> features.copy(bloat = picked)
          }
        picker = null
        onFeatures(next)
      },
    )
  }

  if (confirmBloat) {
    AlertDialog(
      onDismissRequest = { confirmBloat = false },
      containerColor = AppColors.Panel,
      titleContentColor = AppColors.Ink,
      textContentColor = AppColors.InkMuted,
      shape = RoundedCornerShape(18.dp),
      title = { Text("Remove ${features.bloat.size} apps?", style = MaterialTheme.typography.headlineSmall) },
      text = {
        Text(
          "AndroidTweaker removes these apps for the main device user and requests that Android keep their data. System apps can be restored from the list.",
          style = MaterialTheme.typography.bodySmall,
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            confirmBloat = false
            onCommand("bloat")
          },
        ) { Text("REMOVE", style = MaterialTheme.typography.labelLarge, color = AppColors.Rose) }
      },
      dismissButton = {
        TextButton(onClick = { confirmBloat = false }) {
          Text("CANCEL", style = MaterialTheme.typography.labelLarge, color = AppColors.InkMuted)
        }
      },
    )
  }
}
