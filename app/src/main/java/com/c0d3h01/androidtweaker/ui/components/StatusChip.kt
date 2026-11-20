package com.c0d3h01.androidtweaker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.c0d3h01.androidtweaker.ui.theme.AppColors
import com.c0d3h01.androidtweaker.ui.theme.AppType

@Composable
fun IconSquare(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = 38.dp,
  tint: Color = AppColors.InkMuted,
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
        .background(AppColors.PanelRaised)
        .border(1.dp, AppColors.Hairline, RoundedCornerShape(11.dp)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(size * 0.48f))
    }
  }
}

@Composable
fun StatusChip(
  text: String,
  tone: Color = AppColors.InkMuted,
  dot: Boolean = true,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(AppColors.ChipRadius)
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
