package com.c0d3h01.androidtweaker.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.c0d3h01.androidtweaker.data.Features
import com.c0d3h01.androidtweaker.data.Resolution
import com.c0d3h01.androidtweaker.util.ShellAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Which package list an [AppPickerDialog] is editing. */
enum class AppPickerKind(
  val title: String,
  val blurb: String,
) {
  HEAVY("Heavy apps", "Performance game mode and RT priority while they run"),
  LIGHT("Light apps", "Battery game mode for apps that do not need speed"),
  WHITELIST("Doze whitelist", "Apps that must keep running during forced idle"),
  BLOAT("Bloatware", "Uninstall for user 0; every removal can be restored"),
}

data class AppEntry(
  val label: String,
  val pkg: String,
)

/** Adds the FDE-style feature sections to the Tweaks list. */
fun LazyListScope.featureSections(
  features: Features,
  shell: ShellAccess.Mode,
  onFeatures: (Features) -> Unit,
  onCommand: (String) -> Unit,
  onPickApps: (AppPickerKind) -> Unit,
  onRemoveBloat: () -> Unit,
  onRestore: (String) -> Unit,
  onGrantShizuku: () -> Unit,
  onOpenShizuku: () -> Unit,
) {
  val rooted = shell.canApplyProfiles

  item {
    Eyebrow(label = "Health & charging")
  }
  item {
    Panel {
      Text("Charge limit", style = MaterialTheme.typography.titleMedium, color = Bench.Ink)
      Spacer(Modifier.height(2.dp))
      Text(
        if (rooted) {
          "Holds charging at the chosen level and releases 2% lower while the battery monitor runs."
        } else {
          "Needs root: charge nodes are kernel sysfs entries."
        },
        style = MaterialTheme.typography.bodySmall,
        color = Bench.InkFaint,
      )
      Spacer(Modifier.height(10.dp))
      val chargeLevels = listOf(0, 60, 70, 80, 90, 100)
      ChoiceChipRow(
        labels = chargeLevels.map { if (it == 0) "Off" else "$it%" },
        selected = chargeLevels.indexOf(features.chargeLimit),
        onSelect = { index -> if (rooted) onFeatures(features.copy(chargeLimit = chargeLevels[index])) },
      )
      Spacer(Modifier.height(4.dp))
      SwitchRow(
        title = "Charging bypass",
        blurb = "Hold charging while the battery monitor runs.",
        checked = features.chargeBypass,
        enabled = rooted,
        onChange = { onFeatures(features.copy(chargeBypass = it)) },
      )
      Hairline(Modifier.padding(vertical = 2.dp))
      Text("Temperature fuse", style = MaterialTheme.typography.titleMedium, color = Bench.Ink)
      Spacer(Modifier.height(2.dp))
      Text(
        "Holds charging when the pack gets this hot while the battery monitor runs.",
        style = MaterialTheme.typography.bodySmall,
        color = Bench.InkFaint,
      )
      Spacer(Modifier.height(10.dp))
      val fuseTemps = listOf(0, 40, 45, 50, 55)
      ChoiceChipRow(
        labels = fuseTemps.map { if (it == 0) "Off" else "$it\u00A0°C" },
        selected = fuseTemps.indexOf(features.tempFuse),
        onSelect = { index -> if (rooted) onFeatures(features.copy(tempFuse = fuseTemps[index])) },
      )
    }
  }

  item {
    Eyebrow(label = "Thermal") {
      if (!rooted) StatusChip("Root", Bench.Amber, dot = false)
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

  item {
    Eyebrow(label = "Cleanup & system rules")
  }
  item {
    Panel {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GhostButton("Free RAM", onClick = { onCommand("clean ram") }, modifier = Modifier.weight(1f), enabled = rooted)
        GhostButton("Clear cache", onClick = { onCommand("clean cache") }, modifier = Modifier.weight(1f), enabled = rooted)
      }
      Spacer(Modifier.height(8.dp))
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GhostButton("Clear logs", onClick = { onCommand("clean logs") }, modifier = Modifier.weight(1f))
        GhostButton("Recompile ART", onClick = { onCommand("clean art") }, modifier = Modifier.weight(1f))
      }
      Spacer(Modifier.height(6.dp))
      Hairline(Modifier.padding(vertical = 2.dp))
      SwitchRow(
        title = "Reduce debug logging",
        blurb = "Cuts log buffers and verbose kernel/ART tracing.",
        checked = features.disableLogs,
        onChange = { onFeatures(features.copy(disableLogs = it)) },
      )
      Hairline(Modifier.padding(vertical = 2.dp))
      Text("Phantom process limit", style = MaterialTheme.typography.titleMedium, color = Bench.Ink)
      Spacer(Modifier.height(2.dp))
      Text(
        "Caps how many child processes an app may fork in the background.",
        style = MaterialTheme.typography.bodySmall,
        color = Bench.InkFaint,
      )
      Spacer(Modifier.height(10.dp))
      val phantomLimits = listOf(0, 8, 16, 32, 64)
      ChoiceChipRow(
        labels = phantomLimits.map { if (it == 0) "Off" else "$it" },
        selected = phantomLimits.indexOf(features.phantomLimit),
        onSelect = { index -> onFeatures(features.copy(phantomLimit = phantomLimits[index])) },
      )
      Hairline(Modifier.padding(vertical = 10.dp))
      PickerRow(
        label = "Bloatware",
        value = "${features.bloat.size} selected",
        onClick = { onPickApps(AppPickerKind.BLOAT) },
      )
      if (features.bloat.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        Spacer(Modifier.height(2.dp))
        features.bloat.sorted().take(6).forEach { pkg ->
          Row(
            Modifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              pkg,
              style = AppType.mono,
              color = Bench.InkMuted,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            Box(
              Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clickable(
                  role = androidx.compose.ui.semantics.Role.Button,
                  onClickLabel = "Restore $pkg",
                ) { onRestore(pkg) },
              contentAlignment = Alignment.Center,
            ) {
              Text(
                "RESTORE",
                style = AppType.micro,
                color = Bench.Signal,
              )
            }
          }
        }
        if (features.bloat.size > 6) {
          Text("+${features.bloat.size - 6} more", style = AppType.micro, color = Bench.InkFaint)
        }
        Spacer(Modifier.height(8.dp))
        GhostButton("Remove selected apps", onClick = onRemoveBloat)
      }
    }
  }

  item {
    Eyebrow(label = "Device controls")
  }
  item {
    Panel {
      Text("Resolution", style = MaterialTheme.typography.titleMedium, color = Bench.Ink)
      Spacer(Modifier.height(8.dp))
      val resolutions = Resolution.entries
      ChoiceChipRow(
        labels = resolutions.map { it.label },
        selected = resolutions.indexOf(features.resolution),
        onSelect = { index -> onFeatures(features.copy(resolution = resolutions[index])) },
      )
      Hairline(Modifier.padding(vertical = 10.dp))
      Text("Refresh-rate cap", style = MaterialTheme.typography.titleMedium, color = Bench.Ink)
      Spacer(Modifier.height(8.dp))
      val refreshCaps = listOf(0, 60, 90, 120)
      ChoiceChipRow(
        labels = refreshCaps.map { if (it == 0) "Off" else "$it\u00A0Hz" },
        selected = refreshCaps.indexOf(features.peakFps),
        onSelect = { index -> onFeatures(features.copy(peakFps = refreshCaps[index])) },
      )
    }
  }

  item {
    Eyebrow(label = "Network")
  }
  item {
    Panel {
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

  item {
    Eyebrow(label = "App profiles")
  }
  item {
    Panel {
      PickerRow(
        label = "Heavy apps",
        value = "${features.heavyApps.size} apps",
        onClick = { onPickApps(AppPickerKind.HEAVY) },
      )
      Text(
        "Performance game mode and RT priority while they run.",
        style = MaterialTheme.typography.bodySmall,
        color = Bench.InkFaint,
      )
      Hairline(Modifier.padding(vertical = 10.dp))
      PickerRow(
        label = "Light apps",
        value = "${features.lightApps.size} apps",
        onClick = { onPickApps(AppPickerKind.LIGHT) },
      )
      Text(
        "Battery game mode for apps that do not need full speed.",
        style = MaterialTheme.typography.bodySmall,
        color = Bench.InkFaint,
      )
    }
  }

  item {
    Eyebrow(label = "Shell access")
  }
  item {
    Panel {
      when (shell) {
        ShellAccess.Mode.ROOT, ShellAccess.Mode.SHIZUKU_ROOT -> {
          InfoRow("Backend", stringResource(shell.titleRes))
          Spacer(Modifier.height(6.dp))
          Text(
            "Kernel tuning is available on devices that expose writable settings.",
            style = MaterialTheme.typography.bodySmall,
            color = Bench.InkFaint,
          )
        }

        ShellAccess.Mode.SHIZUKU -> {
          InfoRow("Backend", "Shizuku · non-root", Bench.Sky)
          Spacer(Modifier.height(6.dp))
          Text(
            "Settings, cleanup, doze, network and app profiles run as shell. Sysfs groups stay off.",
            style = MaterialTheme.typography.bodySmall,
            color = Bench.InkFaint,
          )
        }

        ShellAccess.Mode.NONE -> {
          InfoRow("Backend", "None", Bench.Rose)
          Spacer(Modifier.height(6.dp))
          Text(
            "Root unlocks every group. Without root, install Shizuku and grant this app access to run the non-root features.",
            style = MaterialTheme.typography.bodySmall,
            color = Bench.InkFaint,
          )
          Spacer(Modifier.height(10.dp))
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton(
              "Grant Shizuku",
              onClick = onGrantShizuku,
              modifier = Modifier.weight(1f),
            )
            GhostButton(
              "Open Shizuku",
              onClick = onOpenShizuku,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PickerRow(
  label: String,
  value: String,
  onClick: () -> Unit,
) {
  Row(
    Modifier
      .fillMaxWidth()
      .heightIn(min = 48.dp)
      .clip(RoundedCornerShape(10.dp))
      .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
      .padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, style = MaterialTheme.typography.titleMedium, color = Bench.Ink)
    Spacer(Modifier.weight(1f))
    Text(value, style = AppType.micro, color = Bench.InkMuted)
    Spacer(Modifier.width(6.dp))
    Text("CHANGE", style = AppType.micro, color = Bench.Signal)
  }
}

/** Searchable multi-select over launchable apps plus anything already selected. */
@Composable
fun AppPickerDialog(
  kind: AppPickerKind,
  selected: Set<String>,
  onDismiss: () -> Unit,
  onSave: (Set<String>) -> Unit,
) {
  val context = LocalContext.current
  var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
  var query by remember { mutableStateOf("") }
  var picked by remember { mutableStateOf(selected) }
  var loading by remember { mutableStateOf(true) }

  LaunchedEffect(kind) {
    apps = withContext(Dispatchers.IO) { loadApps(context, selected) }
    loading = false
  }

  val shown =
    apps.filter {
      query.isBlank() ||
        it.label.contains(query, ignoreCase = true) ||
        it.pkg.contains(query, ignoreCase = true)
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Bench.Panel,
    titleContentColor = Bench.Ink,
    textContentColor = Bench.InkMuted,
    shape = RoundedCornerShape(18.dp),
    title = {
      Column {
        Text(kind.title, style = MaterialTheme.typography.headlineSmall)
        Text(kind.blurb, style = MaterialTheme.typography.bodySmall, color = Bench.InkFaint)
      }
    },
    text = {
      Column {
        BenchSearchField(value = query, onValueChange = { query = it }, placeholder = "Search apps… e.g. YouTube")
        Spacer(Modifier.height(10.dp))
        if (loading) {
          LoadingLine()
        } else {
          LazyColumn(Modifier.heightIn(max = 360.dp)) {
            items(shown, key = { it.pkg }) { app ->
              val checked = app.pkg in picked
              Row(
                Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(9.dp))
                  .toggleable(
                    value = checked,
                    role = androidx.compose.ui.semantics.Role.Checkbox,
                    onValueChange = {
                      picked = if (checked) picked - app.pkg else picked + app.pkg
                    },
                  ).padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Checkbox(
                  checked = checked,
                  onCheckedChange = null,
                  colors =
                    CheckboxDefaults.colors(
                      checkedColor = Bench.Signal,
                      uncheckedColor = Bench.HairlineStrong,
                      checkmarkColor = Bench.Canvas,
                    ),
                )
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                  Text(
                    app.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = Bench.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(app.pkg, style = AppType.mono, color = Bench.InkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
              }
            }
            if (shown.isEmpty()) {
              item { EmptyState("No apps match", "Try another name or package id.") }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = { onSave(picked) }) {
        Text("SAVE", style = MaterialTheme.typography.labelLarge, color = Bench.Signal)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("CANCEL", style = MaterialTheme.typography.labelLarge, color = Bench.InkMuted)
      }
    },
  )
}

/** Launchable apps, plus any selected package the launcher does not list. */
private fun loadApps(
  context: Context,
  selected: Set<String>,
): List<AppEntry> {
  val pm = context.packageManager
  val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
  val entries =
    pm
      .queryIntentActivities(intent, 0)
      .mapNotNull { info ->
        val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
        if (pkg == context.packageName) return@mapNotNull null
        AppEntry(info.loadLabel(pm).toString(), pkg)
      }.distinctBy { it.pkg }
      .toMutableList()
  val known = entries.map { it.pkg }.toSet()
  selected.filter { it !in known }.sorted().forEach { pkg ->
    val label =
      runCatching {
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA)).toString()
      }.getOrDefault(pkg)
    entries += AppEntry(label, pkg)
  }
  return entries.sortedBy { it.label.lowercase() }
}
