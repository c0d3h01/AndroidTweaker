package com.c0d3h01.androidtweaker.ui

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
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.c0d3h01.androidtweaker.data.CoreSample
import com.c0d3h01.androidtweaker.data.CpuCluster
import com.c0d3h01.androidtweaker.data.TelemetrySnapshot
import com.c0d3h01.androidtweaker.data.formatBytes
import com.c0d3h01.androidtweaker.data.formatCurrent
import com.c0d3h01.androidtweaker.data.formatHz
import com.c0d3h01.androidtweaker.data.formatKHz
import com.c0d3h01.androidtweaker.data.formatTemp
import com.c0d3h01.androidtweaker.data.formatVoltage
import com.c0d3h01.androidtweaker.data.key
import com.c0d3h01.androidtweaker.util.clockWithSeconds
import kotlin.math.roundToInt

@Composable
fun HardwareScreen(
  snapshot: TelemetrySnapshot?,
  loading: Boolean,
  cpuHistory: List<Float>,
  clusterHistory: Map<String, List<Float>>,
  onRefresh: () -> Unit,
) {
  val context = LocalContext.current
  LazyColumn(
    Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = Bench.Gutter, end = Bench.Gutter, top = 12.dp, bottom = Bench.BarClearance),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item { Wordmark(onRefresh = onRefresh) }
    item {
      ScreenTitle(title = "Hardware", subtitle = "Live kernel telemetry") {
        if (snapshot != null) StatusChip("Live", Bench.Mint)
      }
    }
    if (snapshot == null) {
      item {
        EmptyState(
          title = "Reading system…",
          body = "CPU, GPU, thermal and memory telemetry will appear here once the first sample lands.",
          icon = Icons.Outlined.DeveloperBoard,
        )
      }
    } else {
      item { PlatformPanel(snapshot) }
      item { CpuPanel(snapshot, cpuHistory) }
      snapshot.cpu.clusters.forEach { cluster ->
        item(key = "cluster-${cluster.label}-${cluster.cores.firstOrNull()?.index}") {
          ClusterPanel(cluster, clusterHistory[cluster.key].orEmpty())
        }
      }
      item {
        GpuPanel(snapshot)
      }
      item {
        ThermalPanel(snapshot)
      }
      item {
        MemoryPanel(snapshot)
      }
      item {
        BatteryPanel(snapshot)
      }
      item {
        Text(
          "SAMPLE ${clockWithSeconds(context, snapshot.capturedAt)}  ·  REFRESHES EVERY 3S",
          style = AppType.micro,
          color = Bench.InkFaint,
        )
      }
    }
  }
}

@Composable
private fun PlatformPanel(snapshot: TelemetrySnapshot) {
  val d = snapshot.device
  Panel(contentPadding = PaddingValues(16.dp)) {
    Eyebrow(label = "Platform") {
      Text("${snapshot.cpu.cores.size} CORES", style = AppType.micro, color = Bench.InkFaint)
    }
    Spacer(Modifier.height(12.dp))
    Text(
      d.soc ?: d.model,
      style = MaterialTheme.typography.headlineMedium,
      color = Bench.Ink,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    Spacer(Modifier.height(3.dp))
    Text(
      listOfNotNull(d.manufacturer, d.model).joinToString(" · "),
      style = AppType.mono,
      color = Bench.InkMuted,
    )
    Spacer(Modifier.height(14.dp))
    Hairline()
    Spacer(Modifier.height(11.dp))
    InfoRow("Kernel", d.kernel ?: "—")
    Spacer(Modifier.height(8.dp))
    InfoRow("Android", "${d.android}  ·  SDK ${d.sdk}")
    Spacer(Modifier.height(8.dp))
    InfoRow("ABI", d.abi ?: "—")
  }
}

@Composable
private fun CpuPanel(
  snapshot: TelemetrySnapshot,
  cpuHistory: List<Float>,
) {
  val cores = snapshot.cpu.cores
  val online = cores.count { it.online }
  val cur = cores.filter { it.online }.mapNotNull { it.curKHz }.maxOrNull()
  val max = cores.mapNotNull { it.maxKHz }.maxOrNull()
  val governor =
    snapshot.cpu.clusters
      .firstOrNull()
      ?.governor
  Panel(contentPadding = PaddingValues(16.dp)) {
    Eyebrow(label = "CPU") {
      StatusChip("$online/${cores.size} online", if (online == cores.size) Bench.Mint else Bench.Amber)
    }
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.Bottom) {
      Text(formatKHz(cur), style = MaterialTheme.typography.headlineLarge, color = Bench.Ink)
      Spacer(Modifier.width(8.dp))
      Text("PEAK", style = AppType.micro, color = Bench.InkFaint, modifier = Modifier.padding(bottom = 5.dp))
      Spacer(Modifier.weight(1f))
      Text(
        governor ?: "—",
        style = AppType.mono,
        color = Bench.InkMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(bottom = 4.dp),
      )
    }
    Spacer(Modifier.height(10.dp))
    Meter(
      fraction = if (max != null && max > 0) (cur ?: 0).toFloat() / max else 0f,
      tone = Bench.Signal,
    )
    Spacer(Modifier.height(12.dp))
    Sparkline(
      cpuHistory.takeLast(40),
      Bench.Signal,
      Modifier.fillMaxWidth().height(34.dp),
      description = "CPU peak frequency history",
    )
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth()) {
      Text("MIN ${formatKHz(cores.mapNotNull { it.minKHz }.minOrNull())}", style = AppType.micro, color = Bench.InkFaint)
      Spacer(Modifier.weight(1f))
      Text("MAX ${formatKHz(max)}", style = AppType.micro, color = Bench.InkFaint)
    }
  }
}

@Composable
private fun ClusterPanel(
  cluster: CpuCluster,
  history: List<Float>,
) {
  val tone =
    when (cluster.label) {
      "PRIME" -> Bench.Signal
      "MID" -> Bench.Sky
      "BIG" -> Bench.Amber
      else -> Bench.Mint
    }
  val first = cluster.cores.firstOrNull()?.index ?: 0
  val last = cluster.cores.lastOrNull()?.index ?: first
  Panel(contentPadding = PaddingValues(16.dp)) {
    Eyebrow(label = cluster.label) {
      StatusChip(cluster.governor ?: "—", tone)
    }
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.Bottom) {
      Text(
        if (first == last) "CORE C$first" else "CORES C$first–C$last",
        style = AppType.micro,
        color = Bench.InkFaint,
      )
      Spacer(Modifier.weight(1f))
      Text(formatKHz(cluster.curKHz), style = MaterialTheme.typography.headlineSmall, color = Bench.Ink)
    }
    Spacer(Modifier.height(9.dp))
    Meter(
      fraction =
        if (cluster.maxKHz != null && cluster.maxKHz > 0) {
          (cluster.curKHz ?: 0).toFloat() / cluster.maxKHz
        } else {
          0f
        },
      tone = tone,
    )
    Spacer(Modifier.height(13.dp))
    ClusterCores(cluster.cores, tone)
    if (history.size > 1) {
      Spacer(Modifier.height(12.dp))
      Sparkline(
        history.takeLast(40),
        tone,
        Modifier.fillMaxWidth().height(26.dp),
        description = "${cluster.label} frequency history",
      )
    }
  }
}

@Composable
private fun ClusterCores(
  cores: List<CoreSample>,
  tone: Color,
) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    cores.chunked(4).forEach { row ->
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        row.forEach { core ->
          CoreTile(core, tone, Modifier.weight(1f))
        }
        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
      }
    }
  }
}

@Composable
private fun CoreTile(
  core: CoreSample,
  tone: Color,
  modifier: Modifier = Modifier,
) {
  val active = core.online
  Column(
    modifier
      .clip(RoundedCornerShape(9.dp))
      .background(Bench.Sunken)
      .border(1.dp, if (active) Bench.Hairline else Bench.Hairline.copy(alpha = 0.5f), RoundedCornerShape(9.dp))
      .padding(horizontal = 9.dp, vertical = 8.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(Modifier.size(4.dp).background(if (active) tone else Bench.InkFaint, CircleShape))
      Spacer(Modifier.width(5.dp))
      Text("C${core.index}", style = AppType.micro, color = if (active) Bench.Ink else Bench.InkFaint)
      Spacer(Modifier.weight(1f))
      Text(if (active) "ON" else "OFF", style = AppType.micro.copy(letterSpacing = 0.2.sp), color = Bench.InkFaint)
    }
    Spacer(Modifier.height(3.dp))
    Text(formatKHz(core.curKHz), style = AppType.monoStrong, color = if (active) Bench.Ink else Bench.InkFaint, maxLines = 1)
  }
}

@Composable
private fun GpuPanel(snapshot: TelemetrySnapshot) {
  val gpu = snapshot.gpu
  if (gpu == null) {
    EmptyState(
      title = "No GPU telemetry",
      body = "This kernel does not expose a readable GPU devfreq node. CPU, thermal and memory data remain live.",
      icon = Icons.Outlined.Memory,
    )
    return
  }
  val clocksExposed = gpu.curHz != null || gpu.maxHz != null || gpu.busyPercent != null
  val fraction =
    if (gpu.maxHz != null && gpu.maxHz > 0) (gpu.curHz ?: 0).toFloat() / gpu.maxHz else 0f
  Panel(contentPadding = PaddingValues(16.dp)) {
    Eyebrow(label = if (clocksExposed) "GPU" else "GPU renderer") {
      when {
        gpu.busyPercent != null -> StatusChip("${gpu.busyPercent}% busy", Bench.Sky)
        clocksExposed -> StatusChip("Online", Bench.Sky)
        else -> StatusChip("No clocks", Bench.InkMuted)
      }
    }
    Spacer(Modifier.height(12.dp))
    Text(
      gpu.model ?: "Graphics",
      style = MaterialTheme.typography.headlineSmall,
      color = Bench.Ink,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    Spacer(Modifier.height(6.dp))
    if (!clocksExposed) {
      Text(
        "Renderer reported by the graphics stack. This kernel exposes no GPU clock or load node.",
        style = AppType.mono,
        color = Bench.InkMuted,
      )
      return@Panel
    }
    Row(verticalAlignment = Alignment.Bottom) {
      Text(formatHz(gpu.curHz), style = MaterialTheme.typography.headlineLarge, color = Bench.Ink)
      Spacer(Modifier.weight(1f))
      Text(gpu.governor ?: "—", style = AppType.mono, color = Bench.InkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    Spacer(Modifier.height(10.dp))
    Meter(fraction, Bench.Sky)
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth()) {
      Text("MIN ${formatHz(gpu.minHz)}", style = AppType.micro, color = Bench.InkFaint)
      Spacer(Modifier.weight(1f))
      Text("MAX ${formatHz(gpu.maxHz)}", style = AppType.micro, color = Bench.InkFaint)
      if (gpu.tempC != null) {
        Spacer(Modifier.weight(1f))
        Text(formatTemp(gpu.tempC), style = AppType.micro, color = tempTone(gpu.tempC))
      }
    }
  }
}

@Composable
private fun ThermalPanel(snapshot: TelemetrySnapshot) {
  val zones = snapshot.thermal
  if (zones.isEmpty()) {
    EmptyState(
      title = "No thermal zones",
      body = "The kernel does not expose thermal_zone readings to the app on this device.",
      icon = Icons.Outlined.Thermostat,
    )
    return
  }
  Panel(contentPadding = PaddingValues(16.dp)) {
    Eyebrow(label = "Thermal") {
      val hottest = zones.maxOfOrNull { it.tempC } ?: 0f
      StatusChip(stateLabel(hottest), tempTone(hottest))
    }
    Spacer(Modifier.height(12.dp))
    TwoColumnGrid(zones) { zone ->
      ThermalTile(zone.name, zone.tempC)
    }
  }
}

@Composable
private fun ThermalTile(
  name: String,
  temp: Float,
) {
  val tone = tempTone(temp)
  Column(
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(Bench.Sunken)
      .border(1.dp, Bench.Hairline, RoundedCornerShape(10.dp))
      .padding(horizontal = 11.dp, vertical = 10.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(Modifier.size(4.dp).background(tone, CircleShape))
      Spacer(Modifier.width(6.dp))
      Text(name.uppercase(), style = AppType.micro, color = Bench.InkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    Spacer(Modifier.height(7.dp))
    Row(verticalAlignment = Alignment.Bottom) {
      Text("%.1f".format(temp), style = AppType.dataSmall, color = Bench.Ink)
      Spacer(Modifier.width(2.dp))
      Text("°C", style = AppType.micro, color = Bench.InkFaint, modifier = Modifier.padding(bottom = 3.dp))
      Spacer(Modifier.weight(1f))
      Text(stateLabel(temp), style = AppType.micro.copy(letterSpacing = 0.4.sp), color = tone)
    }
    Spacer(Modifier.height(8.dp))
    Meter(((temp - 20f) / 75f).coerceIn(0f, 1f), tone, height = 3.dp)
  }
}

@Composable
private fun MemoryPanel(snapshot: TelemetrySnapshot) {
  val mem = snapshot.memory
  val storage = snapshot.storage
  Panel(contentPadding = PaddingValues(16.dp)) {
    Eyebrow(label = "Memory") {
      if (mem !=
        null
      ) {
        StatusChip("${(mem.usedFraction * 100).roundToInt()}% used", if (mem.usedFraction > 0.85f) Bench.Amber else Bench.Mint)
      }
    }
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.Bottom) {
      Text(
        mem?.let { formatBytes(it.usedBytes) } ?: "—",
        style = MaterialTheme.typography.headlineLarge,
        color = Bench.Ink,
      )
      Spacer(Modifier.width(6.dp))
      Text("USED", style = AppType.micro, color = Bench.InkFaint, modifier = Modifier.padding(bottom = 5.dp))
      Spacer(Modifier.weight(1f))
      Text(
        mem?.let { "OF ${formatBytes(it.totalBytes)}" } ?: "",
        style = AppType.micro,
        color = Bench.InkFaint,
        modifier = Modifier.padding(bottom = 5.dp),
      )
    }
    Spacer(Modifier.height(10.dp))
    Meter(mem?.usedFraction ?: 0f, if ((mem?.usedFraction ?: 0f) > 0.85f) Bench.Amber else Bench.Mint)
    Spacer(Modifier.height(13.dp))
    val swapTotal = mem?.swapTotalBytes ?: 0L
    if (swapTotal > 0) {
      val swapFree = mem?.swapFreeBytes ?: 0L
      InfoRow("Swap in use", formatBytes(swapTotal - swapFree), Bench.InkMuted)
      Spacer(Modifier.height(8.dp))
    }
    if (mem?.zramBytes != null && mem.zramBytes > 0) {
      InfoRow("ZRAM device", formatBytes(mem.zramBytes), Bench.InkMuted)
      Spacer(Modifier.height(8.dp))
    }
    InfoRow(
      "Internal storage",
      storage?.let {
        "${formatBytes(it.totalBytes - it.availableBytes)} / ${formatBytes(it.totalBytes)}"
      } ?: "—",
      Bench.InkMuted,
    )
    Spacer(Modifier.height(8.dp))
    InfoRow("Storage free", storage?.let { formatBytes(it.availableBytes) } ?: "—", Bench.InkMuted)
  }
}

@Composable
private fun BatteryPanel(snapshot: TelemetrySnapshot) {
  val b = snapshot.battery
  Panel(contentPadding = PaddingValues(16.dp)) {
    Eyebrow(label = "Battery") {
      StatusChip(b.status ?: "—", if ((b.percent ?: 100) <= 20) Bench.Rose else Bench.Mint)
    }
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.Bottom) {
      Text(b.percent?.toString() ?: "—", style = MaterialTheme.typography.headlineLarge, color = Bench.Ink)
      Spacer(Modifier.width(2.dp))
      Text("%", style = AppType.micro, color = Bench.InkFaint, modifier = Modifier.padding(bottom = 5.dp))
      Spacer(Modifier.weight(1f))
      Text(
        formatTemp(b.tempC),
        style = AppType.monoStrong,
        color = b.tempC?.let { tempTone(it) } ?: Bench.InkFaint,
        modifier = Modifier.padding(bottom = 4.dp),
      )
    }
    Spacer(Modifier.height(10.dp))
    Meter((b.percent ?: 0) / 100f, if ((b.percent ?: 100) <= 20) Bench.Rose else Bench.Mint)
    Spacer(Modifier.height(13.dp))
    InfoRow("Current", formatCurrent(b.currentMa), Bench.InkMuted)
    Spacer(Modifier.height(8.dp))
    InfoRow("Voltage", formatVoltage(b.voltageMv), Bench.InkMuted)
    Spacer(Modifier.height(8.dp))
    InfoRow("Power source", b.plugged ?: "Battery", Bench.InkMuted)
  }
}

private fun tempTone(c: Float): Color =
  when {
    c >= 65f -> Bench.Rose
    c >= 50f -> Bench.Amber
    else -> Bench.Mint
  }

private fun stateLabel(c: Float): String =
  when {
    c >= 65f -> "HOT"
    c >= 50f -> "WARM"
    else -> "COOL"
  }
