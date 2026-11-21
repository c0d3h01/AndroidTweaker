package com.c0d3h01.androidtweaker.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.c0d3h01.androidtweaker.ui.theme.AppColors
import com.c0d3h01.androidtweaker.ui.theme.AppType

@Composable
fun MetricTile(
  label: String,
  value: String,
  unit: String?,
  tone: Color = AppColors.Ink,
  modifier: Modifier = Modifier,
  trend: List<Float> = emptyList(),
  footer: String? = null,
  onClick: (() -> Unit)? = null,
  sparkDescription: String? = null,
) {
  Panel(
    modifier = modifier,
    onClick = onClick,
    container = AppColors.Panel,
    contentPadding = PaddingValues(14.dp),
  ) {
    Text(
      label.uppercase(),
      style = AppType.micro,
      color = AppColors.InkFaint,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Spacer(Modifier.height(7.dp))
    Row(verticalAlignment = Alignment.Bottom) {
      Text(value, style = AppType.dataSmall, color = tone, maxLines = 1, overflow = TextOverflow.Ellipsis)
      if (unit != null) {
        Spacer(Modifier.width(3.dp))
        Text(unit, style = AppType.micro, color = AppColors.InkFaint, modifier = Modifier.padding(bottom = 3.dp))
      }
    }
    if (trend.isNotEmpty()) {
      Spacer(Modifier.height(8.dp))
      Sparkline(trend, tone, Modifier.fillMaxWidth().height(22.dp), description = sparkDescription)
    } else if (footer != null) {
      Spacer(Modifier.height(6.dp))
    }
    if (footer != null) {
      Text(
        footer,
        style = AppType.micro.copy(letterSpacing = 0.06.sp),
        color = AppColors.InkMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun InfoRow(
  label: String,
  value: String,
  tone: Color = AppColors.Ink,
  modifier: Modifier = Modifier,
) {
  Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Text(label.uppercase(), style = AppType.micro, color = AppColors.InkFaint)
    Spacer(Modifier.weight(1f))
    Text(
      value,
      style = AppType.mono,
      color = tone,
      textAlign = TextAlign.End,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
  }
}
