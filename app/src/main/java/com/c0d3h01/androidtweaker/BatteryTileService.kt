package com.c0d3h01.androidtweaker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.c0d3h01.androidtweaker.BatteryMonitorService.MonitorStatus
import com.c0d3h01.androidtweaker.battery.BatteryReader
import com.c0d3h01.androidtweaker.battery.BatteryReading
import com.c0d3h01.androidtweaker.battery.BatteryRepository
import com.c0d3h01.androidtweaker.battery.analysis.drainRate
import com.c0d3h01.androidtweaker.battery.analysis.estimateToFull
import com.c0d3h01.androidtweaker.battery.formatDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Quick Settings tile: live level and time estimate; tap to pause or resume the monitor. */
class BatteryTileService : TileService() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var statusJob: Job? = null
  private val receiver =
    object : BroadcastReceiver() {
      override fun onReceive(
        context: Context,
        intent: Intent,
      ) = update()
    }

  private var listening = false

  override fun onStartListening() {
    super.onStartListening()
    if (!listening) {
      runCatching {
        ContextCompat.registerReceiver(this, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        listening = true
      }
    }
    if (statusJob == null) statusJob = scope.launch { BatteryMonitorService.status.collect { update() } }
  }

  override fun onStopListening() {
    statusJob?.cancel()
    statusJob = null
    unregisterBatteryReceiver()
    super.onStopListening()
  }

  override fun onDestroy() {
    scope.cancel()
    unregisterBatteryReceiver()
    super.onDestroy()
  }

  private fun unregisterBatteryReceiver() {
    if (listening) {
      runCatching { unregisterReceiver(receiver) }
      listening = false
    }
  }

  override fun onClick() {
    val status = BatteryMonitorService.status.value
    if (BatteryRepository.settings(this).monitorEnabled && (status == MonitorStatus.RUNNING || status == MonitorStatus.STARTING)) {
      BatteryMonitorService.stop(this)
    } else {
      BatteryRepository.update(this) { it.copy(monitorEnabled = true) }
      BatteryMonitorService.start(this)
    }
    update()
  }

  override fun onTileAdded() {
    super.onTileAdded()
    update()
  }

  private fun update() {
    val tile = qsTile ?: return
    val reading = runCatching { BatteryReader.read(this) }.getOrNull()
    val level = reading?.level
    val enabled = BatteryRepository.settings(this).monitorEnabled
    val status = BatteryMonitorService.status.value
    val monitoring = enabled && status == MonitorStatus.RUNNING
    tile.label = getString(R.string.tile_battery_label)
    tile.state = if (monitoring) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    val levelText =
      level?.let { getString(R.string.status_level_percent, it) }
        ?: getString(R.string.status_level_unknown)
    tile.contentDescription =
      getString(
        when {
          monitoring -> R.string.tile_desc_monitoring
          !enabled -> R.string.tile_desc_paused
          status == MonitorStatus.STARTING -> R.string.tile_desc_starting
          else -> R.string.tile_desc_stopped
        },
        levelText,
      )
    if (Build.VERSION.SDK_INT >= 29) {
      tile.subtitle =
        when {
          !enabled -> getString(R.string.tile_monitor_paused)
          status == MonitorStatus.STARTING -> getString(R.string.battery_monitor_chip_starting)
          monitoring -> subtitle(reading)
          else -> getString(R.string.tile_monitor_unavailable)
        }
    }
    if (level != null) tile.icon = levelIcon(level)
    tile.updateTile()
  }

  private fun subtitle(reading: BatteryReading?): String {
    if (reading == null) return getString(R.string.status_reading)
    val status =
      getString(
        when {
          reading.isCharging -> R.string.status_charging
          reading.isPlugged -> R.string.status_plugged
          else -> R.string.status_on_battery
        },
      )
    val time =
      when {
        reading.isCharging -> {
          estimateToFull(reading)?.let { getString(R.string.tile_time_to_full, formatDuration(it)) }
        }

        reading.isPlugged -> {
          null
        }

        else -> {
          val rate = drainRate(BatteryRepository.samples(this), reading.screenOn, System.currentTimeMillis())
          if (rate == null || rate <= 0.2f) {
            null
          } else {
            formatDuration(((reading.level ?: 0) / rate * 3_600_000L).toLong())
          }
        }
      }
    return listOfNotNull(status, time).joinToString(" · ")
  }

  private fun levelIcon(level: Int): Icon {
    val size = 96
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = if (level >= 100) 52f else 74f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
    val baseline = size / 2f - (paint.ascent() + paint.descent()) / 2f
    canvas.drawText(level.toString(), size / 2f, baseline, paint)
    return Icon.createWithBitmap(bitmap)
  }
}
