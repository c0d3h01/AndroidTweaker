package com.c0d3h01.androidtweaker.battery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.c0d3h01.androidtweaker.BatteryMonitorService
import com.c0d3h01.androidtweaker.battery.analysis.UsageReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/** Visible-page reads are lifecycle-cancelled; the service owns background recording. */
class BatteryViewModel(application: Application) : AndroidViewModel(application) {
  private val context = application
  private val mutableState = MutableStateFlow(BatteryUiState(
    settings = BatteryRepository.settings(context),
    sessions = BatteryRepository.sessions(context),
    samples = BatteryRepository.samples(context),
    usageAccess = UsageReader.hasAccess(context),
  ))
  val state = mutableState.asStateFlow()

  suspend fun refreshWhileVisible() {
    while (true) {
      val reading = withContext(Dispatchers.IO) { runCatching { BatteryReader.read(context) }.getOrNull() }
      mutableState.update { it.copy(
        reading = reading,
        settings = BatteryRepository.settings(context),
        sessions = BatteryRepository.sessions(context),
        samples = BatteryRepository.samples(context),
        usageAccess = UsageReader.hasAccess(context),
      ) }
      delay(2_500)
    }
  }

  suspend fun refreshUsage() {
    if (!state.value.usageAccess) return
    val since = state.value.sessions.lastOrNull { it.charging }?.end
      ?: (System.currentTimeMillis() - 6 * 3_600_000L)
    val apps = withContext(Dispatchers.IO) { UsageReader.apps(context, since) }
    mutableState.update { it.copy(apps = apps) }
  }

  fun updateSettings(change: (BatterySettings) -> BatterySettings) {
    val settings = BatteryRepository.update(context, change)
    mutableState.update { it.copy(settings = settings) }
  }

  fun setMonitoring(on: Boolean) {
    if (on) {
      BatteryRepository.update(context) { it.copy(monitorEnabled = true) }
      BatteryMonitorService.start(context)
    } else {
      BatteryMonitorService.stop(context)
    }
    mutableState.update { it.copy(settings = BatteryRepository.settings(context)) }
  }

  fun deleteHistory() {
    BatteryRepository.deleteHistory(context)
    mutableState.update { it.copy(
      samples = BatteryRepository.samples(context),
      sessions = BatteryRepository.sessions(context),
    ) }
  }
}
