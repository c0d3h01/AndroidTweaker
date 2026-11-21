package com.c0d3h01.androidtweaker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.c0d3h01.androidtweaker.ui.theme.AppColors
import com.c0d3h01.androidtweaker.ui.theme.AppType

@Composable
fun PrimaryButton(
  text: String,
  icon: ImageVector? = null,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  val inheritedEnabled = LocalControlsEnabled.current
  val controlsEnabled = enabled && inheritedEnabled
  val opacity = if (inheritedEnabled && !enabled) 0.45f else 1f
  Button(
    onClick = onClick,
    enabled = controlsEnabled,
    modifier = modifier.alpha(opacity).fillMaxWidth().height(50.dp),
    shape = RoundedCornerShape(12.dp),
    colors =
      ButtonDefaults.buttonColors(
        containerColor = AppColors.Signal,
        contentColor = Color(0xFF190A04),
        disabledContainerColor = AppColors.PanelRaised,
        disabledContentColor = AppColors.InkFaint,
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
  val inheritedEnabled = LocalControlsEnabled.current
  val controlsEnabled = enabled && inheritedEnabled
  val opacity = if (inheritedEnabled && !enabled) 0.45f else 1f
  OutlinedButton(
    onClick = onClick,
    enabled = controlsEnabled,
    modifier = modifier.alpha(opacity).fillMaxWidth().height(48.dp),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, AppColors.HairlineStrong),
    colors =
      ButtonDefaults.outlinedButtonColors(
        contentColor = AppColors.Ink,
        disabledContentColor = AppColors.InkFaint,
      ),
    contentPadding = PaddingValues(horizontal = 16.dp),
  ) {
    if (icon != null) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (controlsEnabled) AppColors.InkMuted else AppColors.InkFaint)
      Spacer(Modifier.width(8.dp))
    }
    Text(text.uppercase(), style = MaterialTheme.typography.labelLarge, color = if (controlsEnabled) AppColors.InkMuted else AppColors.InkFaint)
  }
}

@Composable
fun SearchField(
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
      .background(AppColors.Sunken)
      .border(1.dp, AppColors.Hairline, shape)
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(Icons.Outlined.Search, contentDescription = null, tint = AppColors.InkFaint, modifier = Modifier.size(17.dp))
    Spacer(Modifier.width(9.dp))
    BasicTextField(
      value = value,
      onValueChange = onValueChange,
      modifier =
        Modifier
          .weight(1f)
          .semantics { contentDescription = placeholder },
      textStyle = AppType.mono.copy(color = AppColors.Ink),
      singleLine = true,
      cursorBrush = SolidColor(AppColors.Signal),
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
      keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
      decorationBox = { inner ->
        if (value.isEmpty()) {
          Text(placeholder, style = AppType.mono, color = AppColors.InkFaint, maxLines = 1)
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
            .background(AppColors.PanelRaised),
          contentAlignment = Alignment.Center,
        ) {
          Icon(Icons.Outlined.Close, contentDescription = null, tint = AppColors.InkMuted, modifier = Modifier.size(13.dp))
        }
      }
    }
  }
}

/**
 * One tab strip for top-level sections, profiles, battery tabs and log sources.
 * The moving pill stays LTR/RTL-aware and every tab keeps a 48dp target.
 */
@Composable
fun TabStrip(
  labels: List<String>,
  selected: Int,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
  secondaryLabels: List<String>? = null,
  activeTint: Color = AppColors.Signal,
) {
  if (labels.isEmpty()) return
  val shape = RoundedCornerShape(14.dp)
  val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
  Box(
    modifier
      .fillMaxWidth()
      .heightIn(min = 56.dp)
      .clip(shape)
      .background(AppColors.GlassFill)
      .border(1.dp, AppColors.GlassBorder, shape)
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
          AppColors.Signal.copy(alpha = 0.12f),
          topLeft = Offset(left, 0f),
          size = Size(slot, size.height),
          cornerRadius = radius,
        )
        drawRoundRect(
          AppColors.Signal.copy(alpha = 0.22f),
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
            targetValue = if (active) activeTint else AppColors.InkMuted,
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
                color = AppColors.InkFaint,
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
  enabled: Boolean = true,
  onClick: () -> Unit,
) {
  val inheritedEnabled = LocalControlsEnabled.current
  val controlsEnabled = enabled && inheritedEnabled
  val opacity = if (inheritedEnabled && !enabled) 0.45f else 1f
  val shape = RoundedCornerShape(9.dp)
  val fill by animateColorAsState(
    targetValue = if (selected) AppColors.Signal.copy(alpha = 0.16f) else AppColors.Sunken,
    animationSpec = tween(200),
    label = "chipFill",
  )
  val border by animateColorAsState(
    targetValue = if (selected) AppColors.Signal.copy(alpha = 0.42f) else AppColors.Hairline,
    animationSpec = tween(200),
    label = "chipBorder",
  )
  Box(
    modifier
      .alpha(opacity)
      .heightIn(min = 48.dp)
      .defaultMinSize(minWidth = 56.dp)
      .clip(shape)
      .background(fill)
      .border(1.dp, border, shape)
      .selectable(enabled = controlsEnabled, selected = selected, role = Role.RadioButton, onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 8.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      label,
      style = AppType.micro.copy(letterSpacing = 0.2.sp),
      color = if (selected) AppColors.Ink else AppColors.InkMuted,
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
  enabled: Boolean = true,
) {
  FlowRow(
    modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    labels.forEachIndexed { index, label ->
      ChoiceChip(enabled = enabled, label = label, selected = index == selected, onClick = { onSelect(index) })
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
  tone: Color = AppColors.Signal,
  onChange: (Boolean) -> Unit = {},
) {
  val inheritedEnabled = LocalControlsEnabled.current
  val controlsEnabled = enabled && inheritedEnabled
  val opacity = if (inheritedEnabled && !enabled) 0.45f else 1f
  Row(
    modifier
      .alpha(opacity)
      .fillMaxWidth()
      .heightIn(min = 48.dp)
      .toggleable(value = checked, enabled = controlsEnabled, role = Role.Switch, onValueChange = onChange)
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
          color = if (controlsEnabled) AppColors.Ink else AppColors.InkFaint,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        if (badge != null) {
          Spacer(Modifier.width(7.dp))
          StatusChip(badge, AppColors.Amber, dot = false)
        }
      }
      if (blurb != null) {
        Spacer(Modifier.height(2.dp))
        Text(
          blurb,
          style = MaterialTheme.typography.bodySmall,
          color = AppColors.InkFaint,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    Spacer(Modifier.width(12.dp))
    Switch(
      checked = checked,
      onCheckedChange = null,
      enabled = controlsEnabled,
      colors =
        androidx.compose.material3.SwitchDefaults.colors(
          checkedThumbColor = AppColors.Canvas,
          checkedTrackColor = AppColors.Signal,
          checkedBorderColor = AppColors.Signal,
          uncheckedThumbColor = AppColors.InkMuted,
          uncheckedTrackColor = AppColors.Sunken,
          uncheckedBorderColor = AppColors.HairlineStrong,
          disabledUncheckedTrackColor = AppColors.Sunken,
          disabledUncheckedThumbColor = AppColors.InkFaint.copy(alpha = 0.4f),
        ),
    )
  }
}
