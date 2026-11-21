package com.c0d3h01.androidtweaker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun AppTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = AppColorScheme, typography = AppTypography, content = content)
}
