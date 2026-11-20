package com.c0d3h01.androidtweaker.util

import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Time text that follows the user's 12/24-hour setting and locale. */
fun clock(
  context: Context,
  at: Long = System.currentTimeMillis(),
): String = DateFormat.getTimeFormat(context).format(Date(at))

/** Clock with seconds, for log rows and sample timestamps. */
fun clockWithSeconds(
  context: Context,
  at: Long,
): String = format(context, null, at, seconds = true)

/** Date and time, for shared diagnostics. */
fun dateTime(
  context: Context,
  at: Long = System.currentTimeMillis(),
): String = format(context, "yMd", at, seconds = true)

/** Short date plus clock, for saved battery sessions. */
fun dateClock(
  context: Context,
  at: Long,
): String = format(context, "yMd", at, seconds = false)

private fun format(
  context: Context,
  date: String?,
  at: Long,
  seconds: Boolean,
): String {
  val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
  val time = if (DateFormat.is24HourFormat(context)) "H" else "h"
  val skeleton = listOfNotNull(date, "${time}m", if (seconds) "s" else null).joinToString("")
  val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
  return SimpleDateFormat(pattern, locale).format(Date(at))
}
