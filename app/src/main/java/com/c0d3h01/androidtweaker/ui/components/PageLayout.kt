package com.c0d3h01.androidtweaker.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.c0d3h01.androidtweaker.ui.theme.AppColors
import com.c0d3h01.androidtweaker.ui.theme.AppType

@Composable
fun Wordmark(onRefresh: (() -> Unit)? = null) {
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(9.dp).background(AppColors.Signal, RoundedCornerShape(2.dp)))
    Spacer(Modifier.width(9.dp))
    Text(
      "ANDROIDTWEAKER",
      style = AppType.micro.copy(letterSpacing = 1.4.sp),
      color = AppColors.Ink,
    )
    Spacer(Modifier.weight(1f))
    if (onRefresh != null) {
      IconSquare(icon = Icons.Outlined.Refresh, label = "Refresh", onClick = onRefresh)
    }
  }
}

@Composable
fun ScreenTitle(
  title: String,
  subtitle: String,
  trailing: @Composable RowScope.() -> Unit = {},
) {
  Column(Modifier.fillMaxWidth()) {
    Eyebrow(label = subtitle, trailing = trailing)
    Spacer(Modifier.height(6.dp))
    Text(
      title,
      style = MaterialTheme.typography.headlineLarge,
      color = AppColors.Ink,
      modifier = Modifier.semantics { heading() },
    )
  }
}

@Composable
fun EmptyState(
  title: String,
  body: String,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  tone: Color = AppColors.InkMuted,
  action: (@Composable () -> Unit)? = null,
) {
  Panel(modifier = modifier, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 24.dp)) {
    if (icon != null) {
      Icon(icon, contentDescription = null, tint = tone, modifier = Modifier.size(22.dp))
      Spacer(Modifier.height(12.dp))
    }
    Text(
      title,
      style = MaterialTheme.typography.titleLarge,
      color = AppColors.Ink,
      modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(6.dp))
    Text(body, style = MaterialTheme.typography.bodySmall, color = AppColors.InkMuted)
    if (action != null) {
      Spacer(Modifier.height(16.dp))
      action()
    }
  }
}

@Composable
fun LoadingLine(modifier: Modifier = Modifier) {
  val transition = rememberInfiniteTransition(label = "loading")
  val x by
    transition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
      label = "loadingX",
    )
  Canvas(modifier.fillMaxWidth().height(2.dp)) {
    drawRect(AppColors.Hairline)
    val segment = size.width * 0.28f
    val start = (size.width + segment) * x - segment
    drawLine(
      AppColors.Signal,
      Offset(start.coerceAtLeast(0f), size.height / 2),
      Offset((start + segment).coerceAtMost(size.width), size.height / 2),
      strokeWidth = size.height,
    )
  }
}

@Composable
fun <T> TwoColumnGrid(
  items: List<T>,
  modifier: Modifier = Modifier,
  spacing: Dp = 10.dp,
  content: @Composable (T) -> Unit,
) {
  Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
    items.chunked(2).forEach { row ->
      Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
        row.forEach { item ->
          Box(Modifier.weight(1f)) { content(item) }
        }
        if (row.size == 1) Spacer(Modifier.weight(1f))
      }
    }
  }
}
