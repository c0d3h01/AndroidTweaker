package com.c0d3h01.androidtweaker.access

import com.c0d3h01.androidtweaker.R

internal val AccessManager.Mode.titleRes: Int
  get() = when (this) {
    AccessManager.Mode.NONE -> R.string.access_stock
    AccessManager.Mode.ROOT -> R.string.access_root
    AccessManager.Mode.SHIZUKU -> R.string.access_shizuku
    AccessManager.Mode.SHIZUKU_ROOT -> R.string.access_shizuku_root
  }
