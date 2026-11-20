package com.c0d3h01.androidtweaker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.c0d3h01.androidtweaker.R
import com.c0d3h01.androidtweaker.data.Features
import com.c0d3h01.androidtweaker.data.Profile
import com.c0d3h01.androidtweaker.data.TweakGroup
import com.c0d3h01.androidtweaker.data.TweakPrefs
import com.c0d3h01.androidtweaker.data.TweakPreset
import com.c0d3h01.androidtweaker.data.TweakSection
import com.c0d3h01.androidtweaker.data.TweakStore
import com.c0d3h01.androidtweaker.util.ShellAccess

/**
 * Per-profile customization: pick the engine groups each mode applies, then
 * override the limits those groups would otherwise take from the profile.
 * Everything saves immediately; the apply button writes the conf the engine
 * sources, so a later boot reapply uses the same settings.
 */
@Composable
fun TweaksScreen(
  currentCode: Int,
  pendingCode: Int?,
  features: Features,
  shell: ShellAccess.Mode,
  onApply: (Profile) -> Unit,
  onFeatures: (Features) -> Unit,
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
  val profile = Profile.entries[index]
  val spec = PROFILE_SPECS.firstOrNull { it.profile == profile }
  val tone = spec?.tone ?: Bench.Signal
  val profileLabels = PROFILE_SPECS.map { stringResource(it.title) }
  var prefs by remember { mutableStateOf(TweakPrefs()) }

  fun reload() {
    prefs = TweakStore.load(context, profile)
  }

  fun update(next: TweakPrefs) {
    prefs = next.sanitized()
    TweakStore.save(context, profile, prefs)
  }

  LaunchedEffect(profile) { reload() }

  LazyColumn(
    Modifier.fillMaxSize(),
    contentPadding =
      PaddingValues(start = Bench.Gutter, end = Bench.Gutter, top = 12.dp, bottom = Bench.BarClearance),
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
            color = Bench.InkMuted,
          )
          Spacer(Modifier.height(8.dp))
          GhostButton(stringResource(R.string.tuning_access_settings), onClick = onOpenAccess)
        }
      }
    }

    item {
      BenchTabStrip(
        labels = listOf("Modes", "Features"),
        selected = tab,
        onSelect = { tab = it },
      )
    }

    if (tab == 0) {
      item { BenchTabStrip(labels = profileLabels, selected = index, onSelect = { index = it }) }

      item {
        Eyebrow(label = "Preset") {
          Text("${prefs.enabledCount} OF ${TweakGroup.entries.size} GROUPS", style = AppType.micro, color = Bench.InkFaint)
        }
      }
      item {
        Panel(contentPadding = PaddingValues(16.dp)) {
          Text(
            if (pendingCode == profile.code) "Applying this profile now…" else "Settings save as you change them.",
            style = MaterialTheme.typography.bodySmall,
            color = Bench.InkMuted,
          )
          Spacer(Modifier.height(12.dp))
          val activePreset =
            when {
              prefs.groups.size == TweakGroup.entries.size -> TweakPreset.ALL
              prefs.groups.isEmpty() -> TweakPreset.NONE
              prefs.groups == TweakGroup.entries.toSet() - TweakGroup.THERMAL -> TweakPreset.RECOMMENDED
              else -> null
            }
          ChoiceChipRow(
            labels = TweakPreset.entries.map { it.label },
            selected = TweakPreset.entries.indexOf(activePreset),
            onSelect = { index -> update(prefs.withPreset(TweakPreset.entries[index])) },
          )
          Spacer(Modifier.height(8.dp))
          Text(
            when {
              prefs.groups.isEmpty() -> "Nothing will be applied to this profile."
              prefs.groups.size == TweakGroup.entries.size -> "Every engine group is applied."
              TweakGroup.THERMAL !in prefs.groups -> "Recommended leaves thermal throttling alone."
              else -> "Only the groups you enabled below are written."
            },
            style = MaterialTheme.typography.bodySmall,
            color = Bench.InkFaint,
          )
        }
      }

      TweakSection.entries.forEach { section ->
        val groups = TweakGroup.entries.filter { it.section == section }
        if (groups.isEmpty()) return@forEach
        item {
          Eyebrow(label = section.label) {
            Text(section.blurb.uppercase(), style = AppType.micro, color = Bench.InkFaint)
          }
        }
        item {
          Panel(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            groups.forEachIndexed { position, group ->
              if (position > 0) Hairline(Modifier.padding(vertical = 2.dp))
              SwitchRow(
                title = group.label,
                blurb = group.blurb,
                checked = group in prefs.groups,
              ) { on ->
                update(
                  prefs.copy(groups = if (on) prefs.groups + group else prefs.groups - group),
                )
              }
            }
          }
        }
      }

      item {
        Eyebrow(label = "Limits") {
          Text("PROFILE DEFAULT UNLESS SET", style = AppType.micro, color = Bench.InkFaint)
        }
      }
      item {
        Panel(contentPadding = PaddingValues(16.dp)) {
          OptionRow(
            label = "CPU ceiling",
            value = prefs.cpuCeiling,
            options = listOf(60, 75, 90, 100),
            format = { "$it%" },
            onPick = { update(prefs.copy(cpuCeiling = it)) },
          )
          Spacer(Modifier.height(14.dp))
          Hairline()
          Spacer(Modifier.height(14.dp))
          OptionRow(
            label = "GPU ceiling",
            value = prefs.gpuCeiling,
            options = listOf(60, 75, 90, 100),
            format = { "$it%" },
            onPick = { update(prefs.copy(gpuCeiling = it)) },
          )
          Spacer(Modifier.height(14.dp))
          Hairline()
          Spacer(Modifier.height(14.dp))
          OptionRow(
            label = "Swappiness",
            value = prefs.swappiness,
            options = listOf(30, 60, 100, 140, 180),
            onPick = { update(prefs.copy(swappiness = it)) },
          )
          Spacer(Modifier.height(14.dp))
          Hairline()
          Spacer(Modifier.height(14.dp))
          OptionRow(
            label = "Read-ahead",
            value = prefs.readAheadKb,
            options = listOf(64, 128, 256, 512, 1024),
            format = { "$it\u00A0KB" },
            onPick = { update(prefs.copy(readAheadKb = it)) },
          )
        }
      }

      item {
        Panel(contentPadding = PaddingValues(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
              Text(
                stringResource(spec?.title ?: R.string.prof_balanced),
                style = MaterialTheme.typography.titleLarge,
                color = Bench.Ink,
              )
              Text(
                "${prefs.enabledCount} groups · ${spec?.tag ?: "—"}",
                style = AppType.micro,
                color = Bench.InkFaint,
              )
            }
            if (pendingCode == profile.code) StatusChip("Applying…", Bench.Signal)
          }
          Spacer(Modifier.height(12.dp))
          PrimaryButton(
            text =
              if (pendingCode ==
                profile.code
              ) {
                "Applying…"
              } else if (shell.canApplyProfiles) {
                "Apply to ${profile.name.lowercase().replaceFirstChar { it.uppercase() }}"
              } else {
                stringResource(R.string.tuning_apply_root_required)
              },
            onClick = { onApply(profile) },
            enabled = !busy && shell.canApplyProfiles,
          )
          Spacer(Modifier.height(8.dp))
          GhostButton("Reset to profile defaults", onClick = { update(TweakPrefs()) })
        }
      }
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
      containerColor = Bench.Panel,
      titleContentColor = Bench.Ink,
      textContentColor = Bench.InkMuted,
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
        ) { Text("REMOVE", style = MaterialTheme.typography.labelLarge, color = Bench.Rose) }
      },
      dismissButton = {
        TextButton(onClick = { confirmBloat = false }) {
          Text("CANCEL", style = MaterialTheme.typography.labelLarge, color = Bench.InkMuted)
        }
      },
    )
  }
}

@Composable
private fun OptionRow(
  label: String,
  value: Int?,
  options: List<Int>,
  onPick: (Int?) -> Unit,
  format: (Int) -> String = { it.toString() },
) {
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Text(label, style = MaterialTheme.typography.titleMedium, color = Bench.Ink)
    Spacer(Modifier.weight(1f))
    Text(value?.let(format) ?: "Profile default", style = AppType.micro, color = if (value == null) Bench.InkFaint else Bench.Signal)
  }
  Spacer(Modifier.height(8.dp))
  ChoiceChipRow(
    labels = listOf("Profile") + options.map(format),
    selected = if (value == null) 0 else options.indexOf(value) + 1,
    onSelect = { index -> onPick(if (index == 0) null else options[index - 1]) },
  )
}
