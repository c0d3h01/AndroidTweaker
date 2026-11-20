package com.c0d3h01.androidtweaker.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Panel(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  onClickLabel: String? = null,
  container: Color = Bench.Panel,
  borderColor: Color = Bench.Hairline,
  contentPadding: PaddingValues = PaddingValues(16.dp),
  content: @Composable ColumnScope.() -> Unit,
) {
  val shape = RoundedCornerShape(Bench.CardRadius)
  val base =
    modifier
      .fillMaxWidth()
      .clip(shape)
      .background(container)
      .border(1.dp, borderColor, shape)
      .let {
        if (onClick != null) {
          it.clickable(role = Role.Button, onClickLabel = onClickLabel, onClick = onClick)
        } else {
          it
        }
      }.padding(contentPadding)
  Column(base, content = content)
}

@Composable
fun Hairline(
  modifier: Modifier = Modifier,
  color: Color = Bench.Hairline,
) {
  Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

@Composable
fun Eyebrow(
  label: String,
  modifier: Modifier = Modifier,
  trailing: @Composable RowScope.() -> Unit = {},
) {
  Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Text(
      label.uppercase(),
      style = AppType.section,
      color = Bench.InkMuted,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.weight(1f))
    trailing()
  }
}

@Composable
fun Wordmark(onRefresh: (() -> Unit)? = null) {
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(9.dp).background(Bench.Signal, RoundedCornerShape(2.dp)))
    Spacer(Modifier.width(9.dp))
    Text(
      "ANDROIDTWEAKER",
      style = AppType.micro.copy(letterSpacing = 1.4.sp),
      color = Bench.Ink,
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
      color = Bench.Ink,
      modifier = Modifier.semantics { heading() },
    )
  }
}

@Composable
fun IconSquare(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = 38.dp,
  tint: Color = Bench.InkMuted,
  enabled: Boolean = true,
) {
  Box(
    modifier
      .size(maxOf(size, 48.dp))
      .alpha(if (enabled) 1f else 0.38f)
      .then(if (enabled) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      Modifier
        .size(size)
        .clip(RoundedCornerShape(11.dp))
        .background(Bench.PanelRaised)
        .border(1.dp, Bench.Hairline, RoundedCornerShape(11.dp)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(size * 0.48f))
    }
  }
}

@Composable
fun StatusChip(
  text: String,
  tone: Color = Bench.InkMuted,
  dot: Boolean = true,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(Bench.ChipRadius)
  Row(
    modifier
      .clip(shape)
      .background(tone.copy(alpha = 0.08f))
      .border(1.dp, tone.copy(alpha = 0.30f), shape)
      .padding(horizontal = 7.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (dot) {
      Box(Modifier.size(5.dp).background(tone, CircleShape))
      Spacer(Modifier.width(5.dp))
    }
    Text(
      text.uppercase(),
      style = AppType.micro.copy(letterSpacing = 0.9.sp),
      color = tone,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
fun Meter(
  fraction: Float,
  tone: Color,
  modifier: Modifier = Modifier,
  height: Dp = 5.dp,
  track: Color = Bench.Hairline,
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
  tone: Color = Bench.Signal,
) {
  Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    repeat(total) { i ->
      Box(
        Modifier
          .weight(1f)
          .height(4.dp)
          .clip(RoundedCornerShape(1.dp))
          .background(if (i < level) tone else Bench.Hairline),
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
      drawLine(Bench.Hairline, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.dp.toPx())
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

@Composable
fun MetricTile(
  label: String,
  value: String,
  unit: String?,
  tone: Color = Bench.Ink,
  modifier: Modifier = Modifier,
  trend: List<Float> = emptyList(),
  footer: String? = null,
  onClick: (() -> Unit)? = null,
  sparkDescription: String? = null,
) {
  Panel(
    modifier = modifier,
    onClick = onClick,
    container = Bench.Panel,
    contentPadding = PaddingValues(14.dp),
  ) {
    Text(
      label.uppercase(),
      style = AppType.micro,
      color = Bench.InkFaint,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Spacer(Modifier.height(7.dp))
    Row(verticalAlignment = Alignment.Bottom) {
      Text(value, style = AppType.dataSmall, color = tone, maxLines = 1, overflow = TextOverflow.Ellipsis)
      if (unit != null) {
        Spacer(Modifier.width(3.dp))
        Text(unit, style = AppType.micro, color = Bench.InkFaint, modifier = Modifier.padding(bottom = 3.dp))
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
        color = Bench.InkMuted,
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
  tone: Color = Bench.Ink,
  modifier: Modifier = Modifier,
) {
  Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Text(label.uppercase(), style = AppType.micro, color = Bench.InkFaint)
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

@Composable
fun PrimaryButton(
  text: String,
  icon: ImageVector? = null,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.fillMaxWidth().height(50.dp),
    shape = RoundedCornerShape(12.dp),
    colors =
      ButtonDefaults.buttonColors(
        containerColor = Bench.Signal,
        contentColor = Color(0xFF190A04),
        disabledContainerColor = Bench.PanelRaised,
        disabledContentColor = Bench.InkFaint,
      ),
    contentPadding = PaddingValues(horizontal = 18.dp),
  ) {
    if (icon != null) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
      Spacer(Modifier.width(8.dp))
    }
    Text(text.uppercase(), style = MaterialTheme.typography.labelLarge)
  }
}

@Composable
fun GhostButton(
  text: String,
  icon: ImageVector? = null,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  OutlinedButton(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.fillMaxWidth().height(48.dp),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, Bench.HairlineStrong),
    colors =
      ButtonDefaults.outlinedButtonColors(
        contentColor = Bench.Ink,
        disabledContentColor = Bench.InkFaint,
      ),
    contentPadding = PaddingValues(horizontal = 16.dp),
  ) {
    if (icon != null) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = Bench.InkMuted)
      Spacer(Modifier.width(8.dp))
    }
    Text(text.uppercase(), style = MaterialTheme.typography.labelLarge, color = Bench.InkMuted)
  }
}

@Composable
fun BenchSearchField(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  modifier: Modifier = Modifier,
) {
  val focusManager = LocalFocusManager.current
  val shape = RoundedCornerShape(11.dp)
  Row(
    modifier
      .fillMaxWidth()
      .heightIn(min = 48.dp)
      .clip(shape)
      .background(Bench.Sunken)
      .border(1.dp, Bench.Hairline, shape)
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(Icons.Outlined.Search, contentDescription = null, tint = Bench.InkFaint, modifier = Modifier.size(17.dp))
    Spacer(Modifier.width(9.dp))
    BasicTextField(
      value = value,
      onValueChange = onValueChange,
      modifier =
        Modifier
          .weight(1f)
          .semantics { contentDescription = placeholder },
      textStyle = AppType.mono.copy(color = Bench.Ink),
      singleLine = true,
      cursorBrush = SolidColor(Bench.Signal),
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
      keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
      decorationBox = { inner ->
        if (value.isEmpty()) {
          Text(placeholder, style = AppType.mono, color = Bench.InkFaint, maxLines = 1)
        }
        inner()
      },
    )
    if (value.isNotEmpty()) {
      Spacer(Modifier.width(6.dp))
      Box(
        Modifier
          .size(48.dp)
          .clickable(role = Role.Button, onClickLabel = "Clear search", onClick = { onValueChange("") }),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(Bench.PanelRaised),
          contentAlignment = Alignment.Center,
        ) {
          Icon(Icons.Outlined.Close, contentDescription = null, tint = Bench.InkMuted, modifier = Modifier.size(13.dp))
        }
      }
    }
  }
}

@Composable
fun EmptyState(
  title: String,
  body: String,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  tone: Color = Bench.InkMuted,
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
      color = Bench.Ink,
      modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(6.dp))
    Text(body, style = MaterialTheme.typography.bodySmall, color = Bench.InkMuted)
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
    drawRect(Bench.Hairline)
    val segment = size.width * 0.28f
    val start = (size.width + segment) * x - segment
    drawLine(
      Bench.Signal,
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

/**
 * One tab strip for top-level sections, profiles, battery tabs and log sources.
 * The moving pill stays LTR/RTL-aware and every tab keeps a 48dp target.
 */
@Composable
fun BenchTabStrip(
  labels: List<String>,
  selected: Int,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
  secondaryLabels: List<String>? = null,
  activeTint: Color = Bench.Signal,
) {
  if (labels.isEmpty()) return
  val shape = RoundedCornerShape(14.dp)
  val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
  Box(
    modifier
      .fillMaxWidth()
      .heightIn(min = 56.dp)
      .clip(shape)
      .background(Bench.GlassFill)
      .border(1.dp, Bench.GlassBorder, shape)
      .padding(4.dp),
  ) {
    val slotIndex = if (rtl) labels.lastIndex - selected else selected
    val slide by animateFloatAsState(
      targetValue = slotIndex.toFloat(),
      animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
      label = "tabPill",
    )
    Box(Modifier.fillMaxWidth()) {
      Canvas(Modifier.matchParentSize()) {
        val slot = size.width / labels.size
        val left = slot * slide
        val radius = CornerRadius(11.dp.toPx())
        drawRoundRect(
          Bench.Signal.copy(alpha = 0.12f),
          topLeft = Offset(left, 0f),
          size = Size(slot, size.height),
          cornerRadius = radius,
        )
        drawRoundRect(
          Bench.Signal.copy(alpha = 0.22f),
          topLeft = Offset(left, 0f),
          size = Size(slot, size.height),
          cornerRadius = radius,
          style = Stroke(1.dp.toPx()),
        )
      }
      Row(Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
          val active = index == selected
          val tint by animateColorAsState(
            targetValue = if (active) activeTint else Bench.InkMuted,
            animationSpec = tween(220),
            label = "tabTint",
          )
          Column(
            Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .selectable(selected = active, role = Role.Tab, onClick = { onSelect(index) })
              .padding(horizontal = 2.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Text(
              label.uppercase(),
              style = AppType.micro.copy(letterSpacing = 0.4.sp),
              color = tint,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (secondaryLabels != null) {
              Text(
                secondaryLabels[index],
                style = AppType.micro.copy(letterSpacing = 0.2.sp),
                color = Bench.InkFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
  }
}

/** Small selectable chip used by every option row on the tweak surfaces. */
@Composable
fun ChoiceChip(
  label: String,
  selected: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val shape = RoundedCornerShape(9.dp)
  val fill by animateColorAsState(
    targetValue = if (selected) Bench.Signal.copy(alpha = 0.16f) else Bench.Sunken,
    animationSpec = tween(200),
    label = "chipFill",
  )
  val border by animateColorAsState(
    targetValue = if (selected) Bench.Signal.copy(alpha = 0.42f) else Bench.Hairline,
    animationSpec = tween(200),
    label = "chipBorder",
  )
  Box(
    modifier
      .heightIn(min = 48.dp)
      .defaultMinSize(minWidth = 56.dp)
      .clip(shape)
      .background(fill)
      .border(1.dp, border, shape)
      .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 8.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      label,
      style = AppType.micro.copy(letterSpacing = 0.2.sp),
      color = if (selected) Bench.Ink else Bench.InkMuted,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

/** A row of choice chips that wraps instead of squeezing labels at large font scales. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChoiceChipRow(
  labels: List<String>,
  selected: Int,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  FlowRow(
    modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    labels.forEachIndexed { index, label ->
      ChoiceChip(label = label, selected = index == selected, onClick = { onSelect(index) })
    }
  }
}

/** Label + optional icon/blurb on the left, switch on the right. */
@Composable
fun SwitchRow(
  title: String,
  checked: Boolean,
  modifier: Modifier = Modifier,
  blurb: String? = null,
  badge: String? = null,
  enabled: Boolean = true,
  icon: ImageVector? = null,
  tone: Color = Bench.Signal,
  onChange: (Boolean) -> Unit = {},
) {
  Row(
    modifier
      .fillMaxWidth()
      .heightIn(min = 48.dp)
      .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
      .padding(vertical = 9.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (icon != null) {
      Box(
        Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(tone.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(icon, contentDescription = null, tint = tone, modifier = Modifier.size(17.dp))
      }
      Spacer(Modifier.width(11.dp))
    }
    Column(Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          title,
          style = MaterialTheme.typography.titleMedium,
          color = if (enabled) Bench.Ink else Bench.InkFaint,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        if (badge != null) {
          Spacer(Modifier.width(7.dp))
          StatusChip(badge, Bench.Amber, dot = false)
        }
      }
      if (blurb != null) {
        Spacer(Modifier.height(2.dp))
        Text(
          blurb,
          style = MaterialTheme.typography.bodySmall,
          color = Bench.InkFaint,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    Spacer(Modifier.width(12.dp))
    Switch(
      checked = checked,
      onCheckedChange = null,
      enabled = enabled,
      colors =
        androidx.compose.material3.SwitchDefaults.colors(
          checkedThumbColor = Bench.Canvas,
          checkedTrackColor = Bench.Signal,
          checkedBorderColor = Bench.Signal,
          uncheckedThumbColor = Bench.InkMuted,
          uncheckedTrackColor = Bench.Sunken,
          uncheckedBorderColor = Bench.HairlineStrong,
          disabledUncheckedTrackColor = Bench.Sunken,
          disabledUncheckedThumbColor = Bench.InkFaint.copy(alpha = 0.4f),
        ),
    )
  }
}
