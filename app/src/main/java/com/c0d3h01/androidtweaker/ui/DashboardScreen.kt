package com.c0d3h01.androidtweaker.ui

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.c0d3h01.androidtweaker.R
import com.c0d3h01.androidtweaker.data.Profile
import com.c0d3h01.androidtweaker.data.RootInfo
import com.c0d3h01.androidtweaker.data.TelemetrySnapshot
import com.c0d3h01.androidtweaker.data.formatBytes
import com.c0d3h01.androidtweaker.data.formatHz
import com.c0d3h01.androidtweaker.data.formatKHz
import com.c0d3h01.androidtweaker.data.formatTemp
import com.c0d3h01.androidtweaker.data.profileLevel
import com.c0d3h01.androidtweaker.util.ShellAccess
import com.c0d3h01.androidtweaker.util.clockWithSeconds

data class ProfileSpec(
  val profile: Profile,
  @StringRes val title: Int,
  @StringRes val desc: Int,
  val level: Int,
  val tag: String,
  val tone: Color,
)

val PROFILE_SPECS =
  listOf(
    ProfileSpec(Profile.BATTERY, R.string.prof_battery, R.string.pdesc_battery, 1, "ENDURANCE", Bench.Mint),
    ProfileSpec(Profile.BALANCED, R.string.prof_balanced, R.string.pdesc_balanced, 2, "DAILY", Bench.Sky),
    ProfileSpec(Profile.PERFORMANCE, R.string.prof_performance, R.string.pdesc_performance, 3, "RESPONSE", Bench.Signal),
    ProfileSpec(Profile.GAMING, R.string.prof_gaming, R.string.pdesc_gaming, 4, "MAX OUTPUT", Bench.Amber),
  )

@Composable
fun DashboardScreen(
  snapshot: TelemetrySnapshot?,
  root: RootInfo?,
  currentCode: Int,
  pendingCode: Int?,
  loading: Boolean,
  cpuHistory: List<Float>,
  onRefresh: () -> Unit,
  onSelect: (Profile) -> Unit,
  onOpenHardware: () -> Unit,
  onOpenTweaks: () -> Unit,
  onOpenAccess: () -> Unit,
  shell: ShellAccess.Mode,
  quickBusy: Boolean,
  onQuickCommand: (String) -> Unit,
) {
  val current = Profile.fromCode(currentCode)
  val activeSpec = PROFILE_SPECS.firstOrNull { it.profile == current }

  LazyColumn(
    Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = Bench.Gutter, end = Bench.Gutter, top = 12.dp, bottom = Bench.BarClearance),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item { Wordmark(onRefresh = onRefresh) }
    item {
      ScreenTitle(title = stringResource(R.string.nav_performance), subtitle = "Profile & live state") {
        StatusChip(
          stringResource(shell.titleRes),
          if (shell.canApplyProfiles) Bench.Mint else Bench.InkMuted,
        )
      }
    }
    if (!shell.canApplyProfiles) {
      item {
        EmptyState(
          title = stringResource(R.string.tuning_apply_root_required),
          body = stringResource(R.string.profile_access_required),
          action = { GhostButton(stringResource(R.string.tuning_access_settings), onClick = onOpenAccess) },
        )
      }
    }

    item { SystemStrip(snapshot = snapshot, root = root) }

    item {
      ActiveProfilePanel(
        code = currentCode,
        spec = activeSpec,
        pending = pendingCode != null,
        governor =
          snapshot
            ?.cpu
            ?.clusters
            ?.firstOrNull()
            ?.governor,
        device = snapshot?.device?.model,
      )
    }

    item {
      Eyebrow(label = "Quick actions") {
        if (quickBusy) StatusChip("Running…", Bench.Signal)
      }
    }
    item {
      Panel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          GhostButton(
            "Free RAM",
            onClick = { onQuickCommand("clean ram") },
            modifier = Modifier.weight(1f),
            enabled = !quickBusy && shell.canApplyProfiles,
          )
          GhostButton(
            "Clear cache",
            onClick = { onQuickCommand("clean cache") },
            modifier = Modifier.weight(1f),
            enabled = !quickBusy && shell.canApplyProfiles,
          )
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          GhostButton(
            "Force doze",
            onClick = { onQuickCommand("doze force") },
            modifier = Modifier.weight(1f),
            enabled = !quickBusy && shell != ShellAccess.Mode.NONE,
          )
          GhostButton(
            "Apply features",
            onClick = { onQuickCommand("feature") },
            modifier = Modifier.weight(1f),
            enabled = !quickBusy && shell != ShellAccess.Mode.NONE,
          )
        }
      }
    }

    item {
      Eyebrow(label = "Live snapshot") {
        Row(
          Modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onOpenHardware)
            .padding(horizontal = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text("OPEN HARDWARE", style = AppType.micro, color = Bench.Signal)
          Text("→", style = AppType.micro, color = Bench.Signal)
        }
      }
    }
    item {
      TelemetryGrid(snapshot, cpuHistory, onOpenHardware)
    }

    item {
      Eyebrow(label = "Profiles") {
        Text(
          if (loading) "READING…" else if (shell.canApplyProfiles) "TAP TO APPLY" else "ROOT REQUIRED",
          style = AppType.micro,
          color = Bench.InkFaint,
        )
      }
    }
    items(PROFILE_SPECS.size) { index ->
      val spec = PROFILE_SPECS[index]
      ProfileRow(
        spec = spec,
        selected = currentCode == spec.profile.code,
        pending = pendingCode == spec.profile.code,
        onSelect = { onSelect(spec.profile) },
      )
    }

    item { CustomizeEntry(onOpenTweaks) }

    item { SystemDetailsPanel(snapshot, root) }

    item {
      Text(
        "PROFILE CHANGES APPLY WITHOUT A REBOOT  ·  NO MODULE REQUIRED",
        style = AppType.micro.copy(letterSpacing = 0.2.sp),
        color = Bench.InkFaint,
        modifier = Modifier.padding(top = 2.dp),
      )
    }
  }
}

@Composable
private fun SystemStrip(
  snapshot: TelemetrySnapshot?,
  root: RootInfo?,
) {
  val rootTone =
    when {
      root == null -> Bench.InkFaint
      root.granted -> Bench.Mint
      else -> Bench.Rose
    }
  val rootText =
    when {
      root == null -> "CHECKING…"
      root.granted -> "GRANTED"
      else -> "DENIED"
    }
  val selinux = root?.selinux?.uppercase() ?: "—"
  Panel(contentPadding = PaddingValues(0.dp)) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
      StripCell("ROOT", rootText, rootTone, Modifier.weight(1f))
      StripDivider()
      StripCell("SELINUX", selinux, if (selinux == "ENFORCING") Bench.Mint else Bench.Amber, Modifier.weight(1f))
      StripDivider()
      StripCell("ENGINE", "ON-DEVICE", Bench.Ink, Modifier.weight(1f))
    }
    Hairline()
    Row(
      Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val device = snapshot?.device
      Text(
        device?.model ?: "Reading device…",
        style = AppType.monoStrong,
        color = Bench.Ink,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      Text(
        device?.let { "ANDROID ${it.android} · SDK ${it.sdk}" } ?: "",
        style = AppType.micro,
        color = Bench.InkFaint,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun StripCell(
  label: String,
  value: String,
  tone: Color,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier.padding(horizontal = 14.dp, vertical = 11.dp),
    verticalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    Text(label, style = AppType.micro, color = Bench.InkFaint)
    Text(value, style = AppType.monoStrong, color = tone, maxLines = 1, overflow = TextOverflow.Ellipsis)
  }
}

@Composable
private fun StripDivider() {
  Box(Modifier.width(1.dp).fillMaxHeight().background(Bench.Hairline))
}

@Composable
private fun ActiveProfilePanel(
  code: Int,
  spec: ProfileSpec?,
  pending: Boolean,
  governor: String?,
  device: String?,
) {
  val tone = spec?.tone ?: Bench.InkFaint
  val name = spec?.let { stringResource(it.title) } ?: "Unknown"
  val desc = spec?.let { stringResource(it.desc) } ?: "Set a profile below to start tuning this device."
  val level = profileLevel(code)
  Panel(
    container = Bench.PanelRaised,
    borderColor = tone.copy(alpha = if (spec != null) 0.42f else 0.16f),
    contentPadding = PaddingValues(18.dp),
  ) {
    Eyebrow(label = "Active profile") {
      Text(
        if (code > 0) "PROFILE $code" else "NOT APPLIED",
        style = AppType.micro,
        color = Bench.InkFaint,
      )
    }
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        name,
        style = MaterialTheme.typography.displayMedium,
        color = Bench.Ink,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      Spacer(Modifier.width(10.dp))
      if (pending) {
        StatusChip("Applying…", Bench.Signal)
      } else if (spec != null) {
        StatusChip("Engaged", tone)
      }
    }
    Spacer(Modifier.height(6.dp))
    Text(desc, style = MaterialTheme.typography.bodySmall, color = Bench.InkMuted)
    Spacer(Modifier.height(16.dp))
    SegmentedMeter(level = level, tone = tone)
    Spacer(Modifier.height(7.dp))
    Row(Modifier.fillMaxWidth()) {
      Text("ECO", style = AppType.micro, color = Bench.InkFaint)
      Spacer(Modifier.weight(1f))
      Text(spec?.tag ?: "—", style = AppType.micro, color = tone)
      Spacer(Modifier.weight(1f))
      Text("MAX", style = AppType.micro, color = Bench.InkFaint)
    }
    Spacer(Modifier.height(14.dp))
    Hairline()
    Spacer(Modifier.height(11.dp))
    Row(Modifier.fillMaxWidth()) {
      Text("SCHEDULER", style = AppType.micro, color = Bench.InkFaint)
      Spacer(Modifier.width(8.dp))
      Text(governor ?: "—", style = AppType.mono, color = Bench.InkMuted)
      Spacer(Modifier.weight(1f))
      Text(
        device ?: "",
        style = AppType.micro,
        color = Bench.InkFaint,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun TelemetryGrid(
  snapshot: TelemetrySnapshot?,
  cpuHistory: List<Float>,
  onOpenHardware: () -> Unit,
) {
  val maxCur =
    snapshot
      ?.cpu
      ?.cores
      ?.filter { it.online }
      ?.mapNotNull { it.curKHz }
      ?.maxOrNull()
  val gpu = snapshot?.gpu
  val battery = snapshot?.battery
  val memory = snapshot?.memory
  TwoColumnGrid(
    items = listOf("cpu", "gpu", "battery", "memory"),
  ) { key ->
    when (key) {
      "cpu" -> {
        MetricTile(
          label = "CPU peak",
          value = formatKHz(maxCur),
          unit = null,
          tone = Bench.Signal,
          trend = cpuHistory.takeLast(28),
          sparkDescription = "CPU peak frequency history",
          footer =
            snapshot
              ?.cpu
              ?.clusters
              ?.firstOrNull()
              ?.governor,
          onClick = onOpenHardware,
        )
      }

      "gpu" -> {
        if (gpu?.curHz != null) {
          MetricTile(
            label = "GPU clock",
            value = formatHz(gpu.curHz),
            unit = null,
            tone = Bench.Sky,
            footer = gpu.model,
            onClick = onOpenHardware,
          )
        } else {
          MetricTile(
            label = "GPU renderer",
            value = gpu?.model?.substringBefore(" (") ?: "—",
            unit = null,
            tone = Bench.Sky,
            footer = if (gpu?.model != null) "clocks not exposed" else null,
            onClick = onOpenHardware,
          )
        }
      }

      "battery" -> {
        MetricTile(
          label = "Battery",
          value = battery?.percent?.toString() ?: "—",
          unit = if (battery?.percent != null) "%" else null,
          tone = if ((battery?.percent ?: 100) <= 20) Bench.Rose else Bench.Mint,
          footer = formatTemp(battery?.tempC),
          onClick = onOpenHardware,
        )
      }

      else -> {
        MetricTile(
          label = "Memory",
          value = memory?.let { "%.1f".format(it.usedBytes / (1L shl 30).toDouble()) } ?: "—",
          unit = if (memory != null) "GB" else null,
          tone = Bench.Ink,
          footer = memory?.let { "of ${formatBytes(it.totalBytes)}" },
          onClick = onOpenHardware,
        )
      }
    }
  }
}

@Composable
private fun ProfileRow(
  spec: ProfileSpec,
  selected: Boolean,
  pending: Boolean,
  onSelect: () -> Unit,
) {
  val bg by animateColorAsState(if (selected) spec.tone.copy(alpha = 0.07f) else Bench.Panel, label = "profileBg")
  val line by animateColorAsState(if (selected) spec.tone else Bench.Hairline, label = "profileLine")
  Panel(
    onClick = onSelect,
    onClickLabel = if (selected) "Profile already active" else "Apply ${spec.profile.name.lowercase()} profile",
    container = bg,
    borderColor = if (selected) spec.tone.copy(alpha = 0.40f) else Bench.Hairline,
    contentPadding = PaddingValues(0.dp),
    modifier = Modifier.semantics { this.selected = selected },
  ) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
      Box(Modifier.width(3.dp).fillMaxHeight().background(line))
      Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp).weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            stringResource(spec.title),
            style = MaterialTheme.typography.titleLarge,
            color = Bench.Ink,
            modifier = Modifier.weight(1f),
          )
          Spacer(Modifier.width(8.dp))
          when {
            pending -> {
              StatusChip("Applying…", Bench.Signal)
            }

            selected -> {
              StatusChip("Engaged", spec.tone)
            }

            else -> {
              Text("SELECT", style = AppType.micro, color = Bench.InkFaint)
              Spacer(Modifier.width(4.dp))
              androidx.compose.material3.Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = Bench.InkFaint,
                modifier = Modifier.width(14.dp).height(14.dp),
              )
            }
          }
        }
        Spacer(Modifier.height(5.dp))
        Text(
          stringResource(spec.desc),
          style = MaterialTheme.typography.bodySmall,
          color = Bench.InkMuted,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(11.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          SegmentedMeter(level = spec.level, tone = spec.tone, modifier = Modifier.weight(1f))
          Spacer(Modifier.width(10.dp))
          Text(spec.tag, style = AppType.micro, color = Bench.InkFaint)
        }
      }
    }
  }
}

@Composable
private fun CustomizeEntry(onClick: () -> Unit) {
  Panel(onClick = onClick, contentPadding = PaddingValues(16.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        Modifier
          .size(38.dp)
          .background(Bench.Signal.copy(alpha = 0.12f), RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center,
      ) {
        androidx.compose.material3.Icon(
          Icons.Outlined.Tune,
          contentDescription = null,
          tint = Bench.Signal,
          modifier = Modifier.size(19.dp),
        )
      }
      Spacer(Modifier.width(12.dp))
      Column(Modifier.weight(1f)) {
        Text("Customize profiles", style = MaterialTheme.typography.titleMedium, color = Bench.Ink)
        Spacer(Modifier.height(2.dp))
        Text(
          "Choose which groups each mode applies, and its limits",
          style = MaterialTheme.typography.bodySmall,
          color = Bench.InkMuted,
          maxLines = 2,
        )
      }
      Spacer(Modifier.width(8.dp))
      androidx.compose.material3.Icon(
        Icons.Outlined.ChevronRight,
        contentDescription = null,
        tint = Bench.InkFaint,
        modifier = Modifier.size(16.dp),
      )
    }
  }
}

@Composable
private fun SystemDetailsPanel(
  snapshot: TelemetrySnapshot?,
  root: RootInfo?,
) {
  val context = LocalContext.current
  Panel(contentPadding = PaddingValues(16.dp)) {
    Eyebrow(label = "System")
    Spacer(Modifier.height(12.dp))
    val device = snapshot?.device
    InfoRow("Device", listOfNotNull(device?.manufacturer, device?.model).joinToString(" ").ifBlank { "—" })
    Spacer(Modifier.height(9.dp))
    InfoRow("SoC", device?.soc ?: "—")
    Spacer(Modifier.height(9.dp))
    InfoRow("Kernel", device?.kernel ?: "—")
    Spacer(Modifier.height(9.dp))
    InfoRow("Root", root?.manager?.let { m -> root.managerVersion?.let { "$m $it" } ?: m } ?: if (root?.granted == false) "Denied" else "—")
    Spacer(Modifier.height(9.dp))
    InfoRow("Tweak engine", "Bundled · direct root")
    Spacer(Modifier.height(13.dp))
    Hairline()
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Box(Modifier.size(6.dp).background(if (root?.granted == true) Bench.Mint else Bench.Rose, RoundedCornerShape(50)))
      Spacer(Modifier.width(7.dp))
      Text(
        if (root?.granted == true) "ROOT SHELL ACTIVE" else "ROOT SHELL UNAVAILABLE",
        style = AppType.micro,
        color = Bench.InkFaint,
      )
      Spacer(Modifier.weight(1f))
      Text(
        snapshot?.let { "UPDATED ${clockWithSeconds(context, it.capturedAt)}" } ?: "—",
        style = AppType.micro,
        color = Bench.InkFaint,
      )
    }
  }
}
