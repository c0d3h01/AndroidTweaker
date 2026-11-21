package com.c0d3h01.androidtweaker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.c0d3h01.androidtweaker.ui.theme.AppColors
import com.c0d3h01.androidtweaker.ui.theme.AppType

/** Shared interaction state for grouped controls, including accessibility actions. */
val LocalControlsEnabled = staticCompositionLocalOf { true }

@Composable
fun Panel(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  onClickLabel: String? = null,
  container: Color = AppColors.Panel,
  borderColor: Color = AppColors.Hairline,
  contentPadding: PaddingValues = PaddingValues(16.dp),
  enabled: Boolean = true,
  content: @Composable ColumnScope.() -> Unit,
) {
  val controlsEnabled = enabled && LocalControlsEnabled.current
  val shape = RoundedCornerShape(AppColors.CardRadius)
  val base =
    modifier
      .fillMaxWidth()
      .alpha(if (controlsEnabled) 1f else 0.45f)
      .semantics { if (!controlsEnabled) disabled() }
      .clip(shape)
      .background(container)
      .border(1.dp, borderColor, shape)
      .let {
        if (onClick != null) {
          it.clickable(enabled = controlsEnabled, role = Role.Button, onClickLabel = onClickLabel, onClick = onClick)
        } else {
          it
        }
      }.padding(contentPadding)
  CompositionLocalProvider(LocalControlsEnabled provides controlsEnabled) {
    Column(base, content = content)
  }
}

@Composable
fun Hairline(
  modifier: Modifier = Modifier,
  color: Color = AppColors.Hairline,
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
      color = AppColors.InkMuted,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.weight(1f))
    trailing()
  }
}
