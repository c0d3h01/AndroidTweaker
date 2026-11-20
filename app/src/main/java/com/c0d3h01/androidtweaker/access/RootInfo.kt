package com.c0d3h01.androidtweaker.access

data class RootInfo(
  val granted: Boolean,
  val manager: String?,
  val managerVersion: String?,
  val selinux: String?,
  val suPath: String?,
  val error: String? = null,
)
