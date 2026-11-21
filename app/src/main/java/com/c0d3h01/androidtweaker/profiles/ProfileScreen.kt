package com.c0d3h01.androidtweaker.profiles

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.c0d3h01.androidtweaker.R
import com.c0d3h01.androidtweaker.access.AccessManager
import com.c0d3h01.androidtweaker.ui.components.ChoiceChipRow
import com.c0d3h01.androidtweaker.ui.components.Eyebrow
import com.c0d3h01.androidtweaker.ui.components.GhostButton
import com.c0d3h01.androidtweaker.ui.components.Hairline
import com.c0d3h01.androidtweaker.ui.components.Panel
import com.c0d3h01.androidtweaker.ui.components.PrimaryButton
import com.c0d3h01.androidtweaker.ui.components.StatusChip
import com.c0d3h01.androidtweaker.ui.components.SwitchRow
import com.c0d3h01.androidtweaker.ui.components.TabStrip
import com.c0d3h01.androidtweaker.ui.theme.AppColors
import com.c0d3h01.androidtweaker.ui.theme.AppType

/** Per-profile group selection and overrides inside the Tuning page. */
internal fun LazyListScope.profileControls(
  profile: Profile,
  spec: ProfileSpec?,
  prefs: ProfileSettings,
  profileLabels: List<String>,
  selectedIndex: Int,
  onSelectIndex: (Int) -> Unit,
  pendingCode: Int?,
  shell: AccessManager.Mode,
  busy: Boolean,
  onApply: (Profile) -> Unit,
  onUpdate: (ProfileSettings) -> Unit,
) {
  item { TabStrip(labels = profileLabels, selected = selectedIndex, onSelect = onSelectIndex) }

  item {
    Eyebrow(label = "Preset") {
      Text("${prefs.enabledCount} OF ${ProfileGroup.entries.size} GROUPS", style = AppType.micro, color = AppColors.InkFaint)
    }
  }
  item {
    Panel(enabled = shell.canApplyProfiles && !busy, contentPadding = PaddingValues(16.dp)) {
      Text(
        if (pendingCode == profile.code) "Applying this profile now…" else "Settings save as you change them.",
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.InkMuted,
      )
      Spacer(Modifier.height(12.dp))
      val activePreset =
        when {
          prefs.groups.size == ProfileGroup.entries.size -> ProfilePreset.ALL
          prefs.groups.isEmpty() -> ProfilePreset.NONE
          prefs.groups == ProfileGroup.entries.toSet() - ProfileGroup.THERMAL -> ProfilePreset.RECOMMENDED
          else -> null
        }
      ChoiceChipRow(
        labels = ProfilePreset.entries.map { it.label },
        selected = ProfilePreset.entries.indexOf(activePreset),
        onSelect = { index -> onUpdate(prefs.withPreset(ProfilePreset.entries[index])) },
      )
      Spacer(Modifier.height(8.dp))
      Text(
        when {
          prefs.groups.isEmpty() -> "Nothing will be applied to this profile."
          prefs.groups.size == ProfileGroup.entries.size -> "Every engine group is applied."
          ProfileGroup.THERMAL !in prefs.groups -> "Recommended leaves thermal throttling alone."
          else -> "Only the groups you enabled below are written."
        },
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.InkFaint,
      )
    }
  }

  ProfileSection.entries.forEach { section ->
    val groups = ProfileGroup.entries.filter { it.section == section }
    if (groups.isEmpty()) return@forEach
    item {
      Eyebrow(label = section.label) {
        Text(section.blurb.uppercase(), style = AppType.micro, color = AppColors.InkFaint)
      }
    }
    item {
      Panel(enabled = shell.canApplyProfiles && !busy, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
        groups.forEachIndexed { position, group ->
          if (position > 0) Hairline(Modifier.padding(vertical = 2.dp))
          SwitchRow(
            title = group.label,
            blurb = group.blurb,
            checked = group in prefs.groups,
          ) { on ->
            onUpdate(
              prefs.copy(groups = if (on) prefs.groups + group else prefs.groups - group),
            )
          }
        }
      }
    }
  }

  item {
    Eyebrow(label = "Limits") {
      Text("PROFILE DEFAULT UNLESS SET", style = AppType.micro, color = AppColors.InkFaint)
    }
  }
  item {
    Panel(enabled = shell.canApplyProfiles && !busy, contentPadding = PaddingValues(16.dp)) {
      OptionRow(
        label = "CPU ceiling",
        value = prefs.cpuCeiling,
        options = listOf(60, 75, 90, 100),
        format = { "$it%" },
        onPick = { onUpdate(prefs.copy(cpuCeiling = it)) },
      )
      Spacer(Modifier.height(14.dp))
      Hairline()
      Spacer(Modifier.height(14.dp))
      OptionRow(
        label = "GPU ceiling",
        value = prefs.gpuCeiling,
        options = listOf(60, 75, 90, 100),
        format = { "$it%" },
        onPick = { onUpdate(prefs.copy(gpuCeiling = it)) },
      )
      Spacer(Modifier.height(14.dp))
      Hairline()
      Spacer(Modifier.height(14.dp))
      OptionRow(
        label = "Swappiness",
        value = prefs.swappiness,
        options = listOf(30, 60, 100, 140, 180),
        onPick = { onUpdate(prefs.copy(swappiness = it)) },
      )
      Spacer(Modifier.height(14.dp))
      Hairline()
      Spacer(Modifier.height(14.dp))
      OptionRow(
        label = "Read-ahead",
        value = prefs.readAheadKb,
        options = listOf(64, 128, 256, 512, 1024),
        format = { "$it\u00A0KB" },
        onPick = { onUpdate(prefs.copy(readAheadKb = it)) },
      )
    }
  }

  item {
    Panel(enabled = shell.canApplyProfiles && !busy, contentPadding = PaddingValues(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
          Text(
            stringResource(spec?.title ?: R.string.prof_balanced),
            style = MaterialTheme.typography.titleLarge,
            color = AppColors.Ink,
          )
          Text(
            "${prefs.enabledCount} groups · ${spec?.tag ?: "—"}",
            style = AppType.micro,
            color = AppColors.InkFaint,
          )
        }
        if (pendingCode == profile.code) StatusChip("Applying…", AppColors.Signal)
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
      GhostButton("Reset to profile defaults", onClick = { onUpdate(ProfileSettings()) })
    }
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
    Text(label, style = MaterialTheme.typography.titleMedium, color = AppColors.Ink)
    Spacer(Modifier.weight(1f))
    Text(value?.let(format) ?: "Profile default", style = AppType.micro, color = if (value == null) AppColors.InkFaint else AppColors.Signal)
  }
  Spacer(Modifier.height(8.dp))
  ChoiceChipRow(
    labels = listOf("Profile") + options.map(format),
    selected = if (value == null) 0 else options.indexOf(value) + 1,
    onSelect = { index -> onPick(if (index == 0) null else options[index - 1]) },
  )
}
