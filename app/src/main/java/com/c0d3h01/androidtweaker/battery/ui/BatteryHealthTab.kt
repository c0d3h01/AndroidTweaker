package com.c0d3h01.androidtweaker.battery.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.c0d3h01.androidtweaker.battery.BatteryReading
import com.c0d3h01.androidtweaker.battery.BatterySession
import com.c0d3h01.androidtweaker.battery.HealthEstimate
import com.c0d3h01.androidtweaker.battery.formatDuration
import com.c0d3h01.androidtweaker.battery.formatMah
import com.c0d3h01.androidtweaker.battery.formatPower
import com.c0d3h01.androidtweaker.common.dateClock
import com.c0d3h01.androidtweaker.ui.components.Eyebrow
import com.c0d3h01.androidtweaker.ui.components.Hairline
import com.c0d3h01.androidtweaker.ui.components.InfoRow
import com.c0d3h01.androidtweaker.ui.components.Meter
import com.c0d3h01.androidtweaker.ui.components.Panel
import com.c0d3h01.androidtweaker.ui.components.StatusChip
import com.c0d3h01.androidtweaker.ui.theme.AppColors
import com.c0d3h01.androidtweaker.ui.theme.AppType

@Composable
internal fun HealthPanel(
  health: HealthEstimate,
  reading: BatteryReading?,
  sessions: List<BatterySession>,
) {
  Panel(container = AppColors.PanelRaised, borderColor = AppColors.Lavender.copy(alpha = 0.22f)) {
    Eyebrow(label = "Battery health") {
      StatusChip(health.confidence, AppColors.Lavender, dot = true)
    }
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.Bottom) {
      Text(
        health.percent?.toString() ?: "—",
        style = MaterialTheme.typography.displayLarge.copy(fontSize = 54.sp, lineHeight = 58.sp),
        color = if (health.percent == null) AppColors.InkFaint else AppColors.Ink,
      )
      if (health.percent != null) {
        Spacer(Modifier.width(4.dp))
        Text("%", style = MaterialTheme.typography.headlineSmall, color = AppColors.InkMuted, modifier = Modifier.padding(bottom = 7.dp))
      }
      Spacer(Modifier.weight(1f))
      StatusChip(health.label, healthTone(health.percent))
    }
    Spacer(Modifier.height(8.dp))
    Text(
      "${formatMah(health.capacityMah)} of ${formatMah(health.designMah ?: reading?.designMah)} design capacity",
      style = MaterialTheme.typography.bodyMedium,
      color = AppColors.InkMuted,
    )
    Spacer(Modifier.height(14.dp))
    Meter((health.percent ?: 0) / 100f, healthTone(health.percent))
    Spacer(Modifier.height(14.dp))
    InfoRow("Measured from", "${health.sessionsUsed} charge sessions")
    Spacer(Modifier.height(7.dp))
    InfoRow("Cycles", health.cycles?.toString() ?: "Not reported")
    Spacer(Modifier.height(7.dp))
    InfoRow("Design capacity", formatMah(reading?.designMah))
    Spacer(Modifier.height(7.dp))
    InfoRow("Phone reports", formatMah(reading?.fullMah))
    Spacer(Modifier.height(12.dp))
    Text(
      when (health.confidence) {
        "Verified" -> "Several clean charges agree closely, so this reading is reliable."
        "Estimated" -> "Enough charges measured, but they disagree. More charges from low will settle it."
        "Approximate" -> "A rough reading. Charge from below 30% to full a few times to improve it."
        else -> "No usable charge yet. Charge at least 15% with the screen off for the first reading."
      },
      style = MaterialTheme.typography.bodySmall,
      color = AppColors.InkMuted,
    )
  }
}

@Composable
internal fun HealthChart(
  sessions: List<BatterySession>,
  designMah: Int?,
) {
  val charges = sessions.filter { it.charging && it.capacityMah != null }.takeLast(16)
  Panel {
    Eyebrow(label = "Measured capacity") {
      Text("${charges.size} CHARGES", style = AppType.micro, color = AppColors.InkFaint)
    }
    Spacer(Modifier.height(12.dp))
    if (charges.size < 2) {
      Text("2 measured charges are needed for the trend.", style = MaterialTheme.typography.bodySmall, color = AppColors.InkMuted)
    } else {
      CapacityBars(charges.map { it.capacityMah!!.toFloat() }, designMah, Modifier.fillMaxWidth().height(110.dp))
      Spacer(Modifier.height(8.dp))
      Row(Modifier.fillMaxWidth()) {
        Text("OLDEST", style = AppType.micro, color = AppColors.InkFaint)
        Spacer(Modifier.weight(1f))
        Text("LATEST", style = AppType.micro, color = AppColors.InkFaint)
      }
    }
  }
}

@Composable
internal fun CapacityBars(
  values: List<Float>,
  designMah: Int?,
  modifier: Modifier = Modifier,
) {
  Canvas(
    modifier.semantics {
      contentDescription =
        buildString {
          append("Capacity trend across ${values.size} charges")
          values.lastOrNull()?.let { append(", latest ${formatMah(it.toInt())}") }
          designMah?.let { append(", design ${formatMah(it)}") }
        }
    },
  ) {
    if (values.isEmpty()) return@Canvas
    val max = (designMah?.toFloat() ?: values.max()).coerceAtLeast(values.max())
    val gap = 3.dp.toPx()
    val width = (size.width - gap * (values.size - 1)) / values.size
    values.forEachIndexed { index, value ->
      val height = (value / max).coerceIn(0f, 1f) * size.height
      val x = index * (width + gap)
      drawRoundRect(
        color = if (index == values.lastIndex) AppColors.Lavender else AppColors.Lavender.copy(alpha = 0.38f),
        topLeft = Offset(x, size.height - height),
        size = Size(width, height),
        cornerRadius =
          androidx.compose.ui.geometry
            .CornerRadius(3.dp.toPx()),
      )
    }
    if (designMah != null) {
      drawLine(
        AppColors.HairlineStrong,
        Offset(0f, size.height * (1f - designMah.toFloat() / max).coerceIn(0f, 1f)),
        Offset(size.width, size.height * (1f - designMah.toFloat() / max).coerceIn(0f, 1f)),
        strokeWidth = 1.dp.toPx(),
      )
    }
  }
}

@Composable
internal fun ChargeSessions(sessions: List<BatterySession>) {
  val charges = sessions.filter { it.charging }.takeLast(8).reversed()
  Panel {
    Eyebrow(label = "Charge sessions") {
      Text("${sessions.count { it.charging }} TOTAL", style = AppType.micro, color = AppColors.InkFaint)
    }
    Spacer(Modifier.height(10.dp))
    if (charges.isEmpty()) {
      Text("No charge sessions yet.", style = MaterialTheme.typography.bodySmall, color = AppColors.InkMuted)
    } else {
      charges.forEachIndexed { index, session ->
        if (index > 0) {
          Spacer(Modifier.height(11.dp))
          Hairline()
          Spacer(Modifier.height(11.dp))
        }
        SessionRow(session)
      }
    }
  }
}

@Composable
internal fun SessionRow(session: BatterySession) {
  val context = LocalContext.current
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(AppColors.Mint.copy(alpha = 0.09f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(Icons.Outlined.BatteryChargingFull, contentDescription = null, tint = AppColors.Mint, modifier = Modifier.size(17.dp))
    }
    Spacer(Modifier.width(11.dp))
    Column(Modifier.weight(1f)) {
      Text(
        "${session.startLevel}% → ${session.endLevel}% · ${formatDuration(session.durationMs)}",
        style = AppType.monoStrong,
        color = AppColors.Ink,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        "${formatMah(session.addedMah.let { if (it > 0) it else null })} added · ${formatPower(session.peakPowerW)} peak",
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.InkMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Spacer(Modifier.width(8.dp))
    Column(horizontalAlignment = Alignment.End) {
      Text("${session.quality}", style = AppType.monoStrong, color = qualityTone(session.quality))
      Text(dateClock(context, session.end), style = AppType.micro, color = AppColors.InkFaint)
    }
  }
}

internal fun healthTone(percent: Int?): Color =
  when {
    percent == null -> AppColors.InkFaint
    percent >= 85 -> AppColors.Mint
    percent >= 70 -> AppColors.Amber
    else -> AppColors.Rose
  }

internal fun qualityTone(score: Int): Color =
  when {
    score >= 80 -> AppColors.Mint
    score >= 55 -> AppColors.Amber
    else -> AppColors.Rose
  }
