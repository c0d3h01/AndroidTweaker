package com.c0d3h01.androidtweaker.battery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.c0d3h01.androidtweaker.battery.BatterySession
import com.c0d3h01.androidtweaker.battery.BatterySummary
import com.c0d3h01.androidtweaker.battery.analysis.qualityTier
import com.c0d3h01.androidtweaker.battery.formatDuration
import com.c0d3h01.androidtweaker.battery.formatMah
import com.c0d3h01.androidtweaker.common.dateClock
import com.c0d3h01.androidtweaker.ui.components.Eyebrow
import com.c0d3h01.androidtweaker.ui.components.GhostButton
import com.c0d3h01.androidtweaker.ui.components.Hairline
import com.c0d3h01.androidtweaker.ui.components.InfoRow
import com.c0d3h01.androidtweaker.ui.components.Panel
import com.c0d3h01.androidtweaker.ui.components.StatusChip
import com.c0d3h01.androidtweaker.ui.theme.AppColors
import com.c0d3h01.androidtweaker.ui.theme.AppType
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun WeekPanel(
  summary: BatterySummary,
  sessions: List<BatterySession>,
) {
  val days =
    (6 downTo 0).map { offset ->
      val end = System.currentTimeMillis() - offset * 24 * 3_600_000L
      val start = end - 24 * 3_600_000L
      val label =
        LocalDate
          .now()
          .minusDays(offset.toLong())
          .dayOfWeek
          .getDisplayName(TextStyle.SHORT, Locale.getDefault())
      label to
        sessions
          .filter { !it.charging && it.end in start..end }
          .sumOf { (it.startLevel - it.endLevel).coerceAtLeast(0) }
    }
  val peak = days.maxOfOrNull { it.second } ?: 0
  Panel {
    Eyebrow(label = "Last 7 days") {
      StatusChip("${summary.drainPercent}% drained", AppColors.Lavender)
    }
    Spacer(Modifier.height(12.dp))
    Row(
      horizontalArrangement = Arrangement.spacedBy(5.dp),
      modifier =
        Modifier
          .fillMaxWidth()
          .height(104.dp)
          .clearAndSetSemantics {
            contentDescription =
              "Last 7 days: " + days.joinToString(", ") { "${it.first} ${it.second} percent" }
          },
    ) {
      days.forEach { (label, value) ->
        Column(
          Modifier.weight(1f).fillMaxHeight(),
          verticalArrangement = Arrangement.Bottom,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
            Box(
              Modifier
                .fillMaxWidth()
                .fillMaxHeight((value / 100f).coerceIn(0.03f, 1f))
                .clip(RoundedCornerShape(4.dp))
                .background(if (value == peak && value > 0) AppColors.Signal else AppColors.Signal.copy(alpha = 0.35f)),
            )
          }
          Spacer(Modifier.height(4.dp))
          Text(
            "$value%",
            style = AppType.micro.copy(letterSpacing = 0.sp),
            color = AppColors.InkFaint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            label,
            style = AppType.micro.copy(letterSpacing = 0.sp),
            color = AppColors.InkFaint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
    Spacer(Modifier.height(14.dp))
    Hairline()
    Spacer(Modifier.height(12.dp))
    InfoRow("Charges", summary.chargeCount.toString())
    Spacer(Modifier.height(7.dp))
    InfoRow("Added", formatMah(summary.addedMah.let { if (it > 0) it else null }))
    Spacer(Modifier.height(7.dp))
    InfoRow(
      "Charge quality",
      summary.averageQuality?.let { "$it · ${qualityTier(it)}" } ?: "—",
      summary.averageQuality?.let { qualityTone(it) } ?: AppColors.InkMuted,
    )
    Spacer(Modifier.height(7.dp))
    InfoRow("Screen on", formatDuration(summary.screenOnMs))
    Spacer(Modifier.height(7.dp))
    InfoRow("Deep sleep", formatDuration(summary.deepSleepMs))
  }
}

@Composable
internal fun SessionHistory(sessions: List<BatterySession>) {
  val context = LocalContext.current
  val recent = sessions.takeLast(20).reversed()
  Panel {
    Eyebrow(label = "Session history") {
      Text("${sessions.size} SAVED", style = AppType.micro, color = AppColors.InkFaint)
    }
    Spacer(Modifier.height(10.dp))
    if (recent.isEmpty()) {
      Text(
        "Sessions appear after the monitor sees a charge or a discharge.",
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.InkMuted,
      )
    } else {
      recent.forEachIndexed { index, session ->
        if (index > 0) {
          Spacer(Modifier.height(11.dp))
          Hairline()
          Spacer(Modifier.height(11.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            Modifier
              .size(32.dp)
              .clip(RoundedCornerShape(10.dp))
              .background((if (session.charging) AppColors.Mint else AppColors.Lavender).copy(alpha = 0.09f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              if (session.charging) Icons.Outlined.BatteryChargingFull else Icons.Outlined.BatteryFull,
              contentDescription = null,
              tint = if (session.charging) AppColors.Mint else AppColors.Lavender,
              modifier = Modifier.size(16.dp),
            )
          }
          Spacer(Modifier.width(10.dp))
          Column(Modifier.weight(1f)) {
            Text(
              "${session.startLevel}% → ${session.endLevel}% · ${formatDuration(session.durationMs)}",
              style = AppType.monoStrong,
              color = AppColors.Ink,
            )
            Text(
              if (session.charging) {
                "${formatMah(session.addedMah.let { if (it > 0) it else null })} added · quality ${session.quality}"
              } else {
                "Screen on ${formatDuration(session.screenOnMs)} · sleep ${formatDuration(session.deepSleepMs)}"
              },
              style = MaterialTheme.typography.bodySmall,
              color = AppColors.InkMuted,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Text(dateClock(context, session.start), style = AppType.micro, color = AppColors.InkFaint)
        }
      }
    }
  }
}

@Composable
internal fun DataPanel(
  onCsv: () -> Unit,
  onBackup: () -> Unit,
  onDelete: () -> Unit,
) {
  Panel {
    Eyebrow(label = "Data") {
      Text("LOCAL ONLY", style = AppType.micro, color = AppColors.InkFaint)
    }
    Spacer(Modifier.height(10.dp))
    GhostButton("Export CSV", Icons.Outlined.Download, onClick = onCsv)
    Spacer(Modifier.height(8.dp))
    GhostButton("Export backup", Icons.Outlined.Share, onClick = onBackup)
    Spacer(Modifier.height(10.dp))
    GhostButton("Delete history", Icons.Outlined.DeleteOutline, onClick = onDelete)
  }
}

@Composable
internal fun InfoNote(text: String) {
  Row(
    Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(AppColors.Sunken)
      .border(1.dp, AppColors.Hairline, RoundedCornerShape(14.dp))
      .padding(13.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Icon(Icons.Outlined.Info, contentDescription = null, tint = AppColors.InkFaint, modifier = Modifier.size(16.dp))
    Spacer(Modifier.width(9.dp))
    Text(text, style = MaterialTheme.typography.bodySmall, color = AppColors.InkMuted)
  }
}
