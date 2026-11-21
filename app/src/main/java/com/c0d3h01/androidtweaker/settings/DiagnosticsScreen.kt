package com.c0d3h01.androidtweaker.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.c0d3h01.androidtweaker.access.RootInfo
import com.c0d3h01.androidtweaker.battery.formatTemp
import com.c0d3h01.androidtweaker.common.clock
import com.c0d3h01.androidtweaker.common.clockWithSeconds
import com.c0d3h01.androidtweaker.common.dateTime
import com.c0d3h01.androidtweaker.device.TelemetrySnapshot
import com.c0d3h01.androidtweaker.device.formatBytes
import com.c0d3h01.androidtweaker.device.formatHz
import com.c0d3h01.androidtweaker.device.formatKHz
import com.c0d3h01.androidtweaker.engine.TweakEngine
import com.c0d3h01.androidtweaker.profiles.Profile
import com.c0d3h01.androidtweaker.ui.components.Eyebrow
import com.c0d3h01.androidtweaker.ui.components.GhostButton
import com.c0d3h01.androidtweaker.ui.components.Hairline
import com.c0d3h01.androidtweaker.ui.components.InfoRow
import com.c0d3h01.androidtweaker.ui.components.LoadingLine
import com.c0d3h01.androidtweaker.ui.components.Panel
import com.c0d3h01.androidtweaker.ui.components.ScreenTitle
import com.c0d3h01.androidtweaker.ui.components.StatusChip
import com.c0d3h01.androidtweaker.ui.components.Wordmark
import com.c0d3h01.androidtweaker.ui.theme.AppColors
import com.c0d3h01.androidtweaker.ui.theme.AppType

@Composable
fun DiagnosticsScreen(
  snapshot: TelemetrySnapshot?,
  root: RootInfo?,
  currentCode: Int,
  lastApply: TweakEngine.Result?,
  enginePath: String,
  logPath: String,
  loading: Boolean,
  activityNotifications: Boolean,
  notificationsAllowed: Boolean,
  onActivityNotificationsChange: (Boolean) -> Unit,
  onRefresh: () -> Unit,
  onRetryRoot: () -> Unit,
) {
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val diagnostics = buildDiagnostics(context, snapshot, root, currentCode, lastApply, enginePath, logPath)

  fun copy() {
    clipboard.setText(AnnotatedString(diagnostics))
    Toast.makeText(context, "Diagnostics copied", Toast.LENGTH_SHORT).show()
  }

  fun share() {
    val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "AndroidTweaker diagnostics")
        putExtra(Intent.EXTRA_TEXT, diagnostics)
      }
    context.startActivity(Intent.createChooser(intent, "Share diagnostics"))
    ActivityNotifications.diagnosticsShared(context)
  }

  LazyColumn(
    Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = AppColors.Gutter, end = AppColors.Gutter, top = 12.dp, bottom = AppColors.BarClearance),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item { Wordmark(onRefresh = onRefresh) }
    item {
      ScreenTitle(title = "Tools", subtitle = "Root & engine state") {
        if (root?.granted == true) {
          StatusChip("Rooted", AppColors.Mint)
        } else if (root != null) {
          StatusChip("No root", AppColors.Rose)
        }
      }
    }

    item { RootPanel(root, snapshot?.device?.kernel, onRetryRoot) }

    item {
      Eyebrow(label = "Tweak engine") {
        Text("ON-DEVICE", style = AppType.micro, color = AppColors.Mint)
      }
    }
    item { EnginePanel(currentCode, lastApply, enginePath, logPath, root) }

    item {
      Eyebrow(label = "Diagnostics") {
        Text("LOCAL ONLY", style = AppType.micro, color = AppColors.InkFaint)
      }
    }
    item {
      Panel(contentPadding = PaddingValues(16.dp)) {
        InfoRow("Selected profile", Profile.fromCode(currentCode)?.label ?: "Not applied")
        Spacer(Modifier.height(8.dp))
        InfoRow("Root shell", if (root?.granted == true) "Active" else "Unavailable", if (root?.granted == true) AppColors.Mint else AppColors.Rose)
        Spacer(Modifier.height(8.dp))
        InfoRow("Engine version", TweakEngine.ENGINE_VERSION)
        Spacer(Modifier.height(14.dp))
        Hairline()
        Spacer(Modifier.height(13.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          GhostButton("Copy", Icons.Outlined.ContentCopy, onClick = { copy() }, modifier = Modifier.weight(1f))
          GhostButton("Share", Icons.Outlined.Share, onClick = { share() }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Text(
          "Tweaks run directly through the app’s persistent root shell. No companion module is installed or required.",
          style = MaterialTheme.typography.bodySmall,
          color = AppColors.InkFaint,
        )
      }
    }

    item {
      Eyebrow(label = "Notifications") {
        Text(
          when {
            !notificationsAllowed -> "SYSTEM BLOCKED"
            activityNotifications -> "QUIET"
            else -> "PAUSED"
          },
          style = AppType.micro,
          color =
            when {
              !notificationsAllowed -> AppColors.Rose
              activityNotifications -> AppColors.Mint
              else -> AppColors.InkFaint
            },
        )
      }
    }
    item {
      NotificationsPanel(
        enabled = activityNotifications,
        allowed = notificationsAllowed,
        currentCode = currentCode,
        onEnabledChange = onActivityNotificationsChange,
      )
    }
  }
}

@Composable
private fun NotificationsPanel(
  enabled: Boolean,
  allowed: Boolean,
  currentCode: Int,
  onEnabledChange: (Boolean) -> Unit,
) {
  val context = LocalContext.current
  val active = enabled && allowed
  val tone =
    when {
      !allowed -> AppColors.Rose
      enabled -> AppColors.Signal
      else -> AppColors.InkMuted
    }
  val profile = Profile.fromCode(currentCode)?.label
  val previewBody = "${profile ?: "Your profile"} is active · ${clock(context)}"

  fun openSettings() {
    if (!ActivityNotifications.openSettings(context)) {
      Toast.makeText(context, "Open system settings to allow notifications", Toast.LENGTH_SHORT).show()
    }
  }

  Panel(
    container = AppColors.PanelRaised,
    borderColor = tone.copy(alpha = 0.30f),
    contentPadding = PaddingValues(18.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        Modifier
          .size(42.dp)
          .background(tone.copy(alpha = 0.09f), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          if (active) Icons.Outlined.NotificationsActive else Icons.Outlined.Notifications,
          contentDescription = null,
          tint = tone,
          modifier = Modifier.size(21.dp),
        )
      }
      Spacer(Modifier.width(13.dp))
      Column(Modifier.weight(1f)) {
        Text("DELIVERY", style = AppType.micro, color = AppColors.InkFaint)
        Text("Activity updates", style = MaterialTheme.typography.headlineSmall, color = AppColors.Ink)
      }
      Spacer(Modifier.width(10.dp))
      Switch(
        checked = enabled,
        onCheckedChange = onEnabledChange,
        modifier = Modifier.semantics { contentDescription = "Activity notifications" },
        colors =
          SwitchDefaults.colors(
            checkedThumbColor = AppColors.Canvas,
            checkedTrackColor = AppColors.Signal,
            uncheckedThumbColor = AppColors.InkMuted,
            uncheckedTrackColor = AppColors.PanelRaised,
            uncheckedBorderColor = AppColors.HairlineStrong,
          ),
      )
    }
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      StatusChip(
        when {
          !allowed -> "Blocked"
          enabled -> "On"
          else -> "Paused"
        },
        when {
          !allowed -> AppColors.Rose
          enabled -> AppColors.Mint
          else -> AppColors.InkMuted
        },
      )
      Spacer(Modifier.width(9.dp))
      Text(
        "1 silent slot · no sound, vibration, or badge",
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.InkFaint,
        modifier = Modifier.weight(1f),
      )
    }
    Spacer(Modifier.height(14.dp))
    Hairline()
    Spacer(Modifier.height(13.dp))
    Text(
      "AndroidTweaker speaks up only when something finishes. Taps, copies, refreshes, and browsing never notify, so each update marks real work instead of routine use.",
      style = MaterialTheme.typography.bodySmall,
      color = AppColors.InkMuted,
    )
    Spacer(Modifier.height(16.dp))
    Text("WHAT TRIGGERS IT", style = AppType.micro, color = AppColors.InkFaint)
    Spacer(Modifier.height(10.dp))
    NotificationEvent(
      Icons.Outlined.Bolt,
      "Profile applied",
      "A profile finishes applying from Tweaks. The update names the profile and the time.",
      AppColors.Signal,
    )
    Spacer(Modifier.height(12.dp))
    NotificationEvent(
      Icons.Outlined.Description,
      "Log ready to share",
      "A log body opens in the share sheet; the update states how many lines it contains.",
      AppColors.Mint,
    )
    Spacer(Modifier.height(12.dp))
    NotificationEvent(
      Icons.Outlined.Share,
      "Diagnostics ready to share",
      "The device, root, and engine summary opens in the share sheet.",
      AppColors.Sky,
    )
    Spacer(Modifier.height(16.dp))
    Hairline()
    Spacer(Modifier.height(15.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text("PREVIEW", style = AppType.micro, color = AppColors.InkFaint)
      Spacer(Modifier.weight(1f))
      Text(
        when {
          !allowed -> "BLOCKED"
          active -> "LIVE LOOK"
          else -> "PAUSED"
        },
        style = AppType.micro,
        color =
          when {
            !allowed -> AppColors.Rose
            active -> AppColors.Mint
            else -> AppColors.InkFaint
          },
      )
    }
    Spacer(Modifier.height(9.dp))
    Box(
      Modifier
        .fillMaxWidth()
        .alpha(if (active) 1f else 0.45f)
        .clip(RoundedCornerShape(14.dp))
        .background(AppColors.Sunken)
        .border(1.dp, AppColors.Hairline, RoundedCornerShape(14.dp))
        .padding(12.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AppColors.SignalSoft),
          contentAlignment = Alignment.Center,
        ) {
          Icon(Icons.Outlined.Bolt, contentDescription = null, tint = AppColors.Signal, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ANDROIDTWEAKER", style = AppType.micro, color = AppColors.InkMuted)
            Spacer(Modifier.width(6.dp))
            Text("· NOW", style = AppType.micro, color = AppColors.InkFaint)
          }
          Spacer(Modifier.height(2.dp))
          Text("Profile applied", style = MaterialTheme.typography.titleMedium, color = AppColors.Ink)
          Text(
            previewBody,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.InkMuted,
          )
        }
      }
    }
    Spacer(Modifier.height(8.dp))
    Text(
      "Exactly what appears in the shade. Each update replaces the previous one instead of stacking up.",
      style = MaterialTheme.typography.bodySmall,
      color = AppColors.InkFaint,
    )
    Spacer(Modifier.height(14.dp))
    InfoRow("Sound", "Silent")
    Spacer(Modifier.height(7.dp))
    InfoRow("Badge", "Hidden")
    Spacer(Modifier.height(7.dp))
    InfoRow("Tap action", "Opens Tweaks")
    if (!allowed) {
      Spacer(Modifier.height(15.dp))
      NotificationBlockedCard(onOpenSettings = { openSettings() })
    } else {
      Spacer(Modifier.height(12.dp))
      Text(
        "The boot reapply service keeps its own progress notification. This switch controls activity updates only.",
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.InkFaint,
      )
    }
  }
}

@Composable
private fun NotificationEvent(
  icon: ImageVector,
  title: String,
  body: String,
  tone: Color,
) {
  Row(verticalAlignment = Alignment.Top) {
    Box(
      Modifier
        .size(30.dp)
        .clip(RoundedCornerShape(9.dp))
        .background(tone.copy(alpha = 0.08f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(icon, contentDescription = null, tint = tone, modifier = Modifier.size(15.dp))
    }
    Spacer(Modifier.width(12.dp))
    Column(Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.titleMedium, color = AppColors.Ink)
      Spacer(Modifier.height(2.dp))
      Text(body, style = MaterialTheme.typography.bodySmall, color = AppColors.InkMuted)
    }
  }
}

@Composable
private fun NotificationBlockedCard(onOpenSettings: () -> Unit) {
  Column(
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(AppColors.Amber.copy(alpha = 0.07f))
      .border(1.dp, AppColors.Amber.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
      .padding(13.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = AppColors.Amber, modifier = Modifier.size(16.dp))
      Spacer(Modifier.width(8.dp))
      Text("Blocked by Android", style = MaterialTheme.typography.titleMedium, color = AppColors.Amber)
    }
    Spacer(Modifier.height(6.dp))
    Text(
      "AndroidTweaker can only post updates while notifications are allowed for the app and its Activity updates channel. Allow them in system settings, then leave this switch on.",
      style = MaterialTheme.typography.bodySmall,
      color = AppColors.InkMuted,
    )
    Spacer(Modifier.height(11.dp))
    GhostButton("Open system settings", Icons.Outlined.Settings, onClick = onOpenSettings)
  }
}

@Composable
private fun EnginePanel(
  currentCode: Int,
  lastApply: TweakEngine.Result?,
  enginePath: String,
  logPath: String,
  root: RootInfo?,
) {
  val profile = Profile.fromCode(currentCode)
  val lastText =
    when {
      lastApply == null -> "Not run this session"
      lastApply.ok -> "Applied ${clockWithSeconds(LocalContext.current, lastApply.at)}"
      else -> "Failed · exit ${lastApply.exitCode}"
    }
  val lastTone =
    when {
      lastApply == null -> AppColors.InkMuted
      lastApply.ok -> AppColors.Mint
      else -> AppColors.Rose
    }
  Panel(container = AppColors.PanelRaised, contentPadding = PaddingValues(18.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        Modifier
          .size(42.dp)
          .background(AppColors.Signal.copy(alpha = 0.09f), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(Icons.Outlined.Memory, contentDescription = null, tint = AppColors.Signal, modifier = Modifier.size(21.dp))
      }
      Spacer(Modifier.width(13.dp))
      Column(Modifier.weight(1f)) {
        Text("ENGINE", style = AppType.micro, color = AppColors.InkFaint)
        Text(
          "Direct root",
          style = MaterialTheme.typography.headlineSmall,
          color = AppColors.Ink,
        )
      }
      StatusChip(
        when {
          root?.granted != true -> "Blocked"
          lastApply?.ok == true -> "Ready"
          lastApply != null -> "Error"
          else -> "Idle"
        },
        when {
          root?.granted != true -> AppColors.Rose
          lastApply?.ok == true -> AppColors.Mint
          lastApply != null -> AppColors.Rose
          else -> AppColors.InkMuted
        },
      )
    }
    Spacer(Modifier.height(14.dp))
    Hairline()
    Spacer(Modifier.height(13.dp))
    InfoRow("Selected", profile?.label ?: "Not applied", if (profile != null) AppColors.Ink else AppColors.InkMuted)
    Spacer(Modifier.height(8.dp))
    InfoRow("Last run", lastText, lastTone)
    Spacer(Modifier.height(8.dp))
    InfoRow(
      "Boot reapply",
      if (profile != null) "Enabled" else "Waiting",
      if (profile != null) AppColors.Mint else AppColors.InkMuted,
    )
    Spacer(Modifier.height(8.dp))
    InfoRow("Engine files", enginePath)
    Spacer(Modifier.height(8.dp))
    InfoRow("Log", logPath)
    if (lastApply != null && !lastApply.ok && lastApply.output.isNotBlank()) {
      Spacer(Modifier.height(12.dp))
      Text(
        lastApply.output.trim().takeLast(600),
        style = AppType.log,
        color = AppColors.Rose,
      )
    }
  }
}

@Composable
private fun RootPanel(
  root: RootInfo?,
  kernel: String?,
  onRetry: () -> Unit,
) {
  val granted = root?.granted == true
  val checking = root == null
  val tone =
    when {
      checking -> AppColors.InkFaint
      granted -> AppColors.Mint
      else -> AppColors.Rose
    }
  Panel(
    container = AppColors.PanelRaised,
    borderColor = tone.copy(alpha = if (checking) 0.16f else 0.38f),
    contentPadding = PaddingValues(18.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        Modifier
          .size(42.dp)
          .background(tone.copy(alpha = 0.09f), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(Icons.Outlined.Security, contentDescription = null, tint = tone, modifier = Modifier.size(21.dp))
      }
      Spacer(Modifier.width(13.dp))
      Column(Modifier.weight(1f)) {
        Text("ROOT ACCESS", style = AppType.micro, color = AppColors.InkFaint)
        Text(
          when {
            checking -> "CHECKING…"
            granted -> "GRANTED"
            else -> "DENIED"
          },
          style = MaterialTheme.typography.headlineMedium,
          color = if (checking) AppColors.InkMuted else tone,
        )
      }
      when {
        checking -> LoadingLine(Modifier.width(48.dp))
        granted -> StatusChip("Active", tone)
        else -> StatusChip("Blocked", tone)
      }
    }
    Spacer(Modifier.height(14.dp))
    Hairline()
    Spacer(Modifier.height(13.dp))
    InfoRow(
      "Provider",
      root?.manager?.let { m -> root.managerVersion?.let { "$m $it" } ?: m } ?: if (checking) "Reading…" else "Unavailable",
    )
    Spacer(Modifier.height(8.dp))
    InfoRow("SELinux", root?.selinux ?: "—", if (root?.selinux.equals("Enforcing", true)) AppColors.Mint else AppColors.Amber)
    Spacer(Modifier.height(8.dp))
    InfoRow("su binary", root?.suPath ?: "—")
    Spacer(Modifier.height(8.dp))
    InfoRow("Kernel", kernel ?: "—")
    if (!granted && !checking) {
      Spacer(Modifier.height(14.dp))
      Text(
        "AndroidTweaker needs superuser access to apply tweaks and read system logs. Grant it in your root manager, then retry.",
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.InkMuted,
      )
      Spacer(Modifier.height(12.dp))
      GhostButton("Retry root", onClick = onRetry)
    }
  }
}

private val Profile.label: String
  get() = name.lowercase().replaceFirstChar { it.uppercase() }

private fun buildDiagnostics(
  context: Context,
  snapshot: TelemetrySnapshot?,
  root: RootInfo?,
  currentCode: Int,
  lastApply: TweakEngine.Result?,
  enginePath: String,
  logPath: String,
): String {
  val time = dateTime(context)
  val sb = StringBuilder()
  sb.appendLine("AndroidTweaker diagnostics")
  sb.appendLine("Generated: $time")
  val d = snapshot?.device
  if (d != null) {
    sb.appendLine("Device: ${d.manufacturer.orEmpty()} ${d.model}")
    sb.appendLine("SoC: ${d.soc ?: "—"}")
    sb.appendLine("Android: ${d.android} (SDK ${d.sdk}), ${d.abi ?: "—"}")
    sb.appendLine("Kernel: ${d.kernel ?: "—"}")
  }
  sb.appendLine(
    "Root: ${if (root?.granted == true) "granted" else "unavailable"} (${root?.manager ?: "unknown"} ${root?.managerVersion.orEmpty()})",
  )
  sb.appendLine("SELinux: ${root?.selinux ?: "—"}")
  sb.appendLine("Selected profile: ${Profile.fromCode(currentCode)?.label ?: "not applied"}")
  sb.appendLine("Engine: on-device v${TweakEngine.ENGINE_VERSION} ($enginePath)")
  sb.appendLine("Last apply: ${lastApply?.let { if (it.ok) "ok" else "failed (${it.exitCode})" } ?: "not run this session"}")
  if (snapshot != null) {
    sb.appendLine("CPU cores: ${snapshot.cpu.cores.count { it.online }}/${snapshot.cpu.cores.size} online")
    snapshot.cpu.clusters.forEach { c ->
      sb.appendLine("  ${c.label}: ${formatKHz(c.curKHz)} / ${formatKHz(c.maxKHz)} (${c.governor ?: "—"})")
    }
    sb.appendLine("GPU: ${snapshot.gpu?.model ?: "—"} ${formatHz(snapshot.gpu?.curHz)}")
    sb.appendLine("Thermal: ${snapshot.thermal.joinToString { "${it.name} ${formatTemp(it.tempC)}" }}")
    sb.appendLine("Memory: ${formatBytes(snapshot.memory?.usedBytes)} / ${formatBytes(snapshot.memory?.totalBytes)}")
    sb.appendLine("Battery: ${snapshot.battery.percent ?: "—"}% ${formatTemp(snapshot.battery.tempC)}")
  }
  sb.appendLine("Log: $logPath")
  return sb.toString().trim()
}
