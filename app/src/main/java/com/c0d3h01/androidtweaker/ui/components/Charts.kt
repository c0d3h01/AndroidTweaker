package com.c0d3h01.androidtweaker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.c0d3h01.androidtweaker.ui.theme.AppColors

@Composable
fun Meter(
  fraction: Float,
  tone: Color,
  modifier: Modifier = Modifier,
  height: Dp = 5.dp,
  track: Color = AppColors.Hairline,
) {
  val animated by animateFloatAsState(
    fraction.coerceIn(0f, 1f),
    animationSpec = tween(600, easing = androidx.compose.animation.core.FastOutSlowInEasing),
    label = "meter",
  )
  Canvas(modifier.fillMaxWidth().height(height)) {
    val radius = CornerRadius(size.height / 2)
    drawRoundRect(track, cornerRadius = radius)
    drawRoundRect(
      tone,
      size = Size(size.width * animated, size.height),
      cornerRadius = radius,
    )
  }
}

@Composable
fun SegmentedMeter(
  level: Int,
  modifier: Modifier = Modifier,
  total: Int = 4,
  tone: Color = AppColors.Signal,
) {
  Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    repeat(total) { i ->
      Box(
        Modifier
          .weight(1f)
          .height(4.dp)
          .clip(RoundedCornerShape(1.dp))
          .background(if (i < level) tone else AppColors.Hairline),
      )
    }
  }
}

@Composable
fun Sparkline(
  values: List<Float>,
  color: Color,
  modifier: Modifier = Modifier,
  fill: Boolean = true,
  description: String? = null,
) {
  Canvas(
    modifier.semantics {
      if (description != null) contentDescription = description
    },
  ) {
    if (values.size < 2) {
      drawLine(AppColors.Hairline, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.dp.toPx())
      return@Canvas
    }
    val min = values.minOrNull() ?: 0f
    val max = values.maxOrNull() ?: 1f
    val flat = max - min < 0.0001f
    val range = (max - min).coerceAtLeast(0.0001f)
    val step = size.width / (values.size - 1)
    val path =
      Path().apply {
        values.forEachIndexed { index, value ->
          val x = index * step
          val y = if (flat) size.height / 2 else size.height - ((value - min) / range) * size.height
          if (index == 0) moveTo(x, y) else lineTo(x, y)
        }
      }
    if (fill) {
      val fillPath =
        Path().apply {
          addPath(path)
          lineTo(size.width, size.height)
          lineTo(0f, size.height)
          close()
        }
      drawPath(
        fillPath,
        Brush.verticalGradient(listOf(color.copy(alpha = 0.16f), Color.Transparent)),
      )
    }
    drawPath(
      path,
      color,
      style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
  }
}
