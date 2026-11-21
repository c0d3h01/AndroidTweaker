package com.c0d3h01.androidtweaker.device

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HardwareState(
  val snapshot: TelemetrySnapshot? = null,
  val cpuHistory: List<Float> = emptyList(),
  val clusterHistory: Map<String, List<Float>> = emptyMap(),
  val loading: Boolean = false,
)

/** Samples only when requested by the visible hardware/performance route. */
class HardwareViewModel(application: Application) : AndroidViewModel(application) {
  private val context = application
  private val mutableHardware = MutableStateFlow(HardwareState())
  val hardware = mutableHardware.asStateFlow()
  fun refreshHardware(onUnavailable: () -> Unit = {}) {
    viewModelScope.launch { sampleHardware(onUnavailable) }
  }

  suspend fun sampleHardware(onUnavailable: () -> Unit = {}) {
    if (hardware.value.loading) return
    mutableHardware.update { it.copy(loading = true) }
    try {
      val sample = HardwareReader.read(context)
      val peak = sample.cpu.cores.asSequence().filter { it.online }.mapNotNull { it.curKHz }.maxOrNull()
      mutableHardware.update { current ->
        val clusterHistory = sample.cpu.clusters.associate { cluster ->
          val previous = current.clusterHistory[cluster.key].orEmpty()
          cluster.key to (cluster.curKHz?.let { (previous + it.toFloat()).takeLast(HISTORY_SIZE) } ?: previous)
        }
        current.copy(
          snapshot = sample,
          cpuHistory = peak?.let { (current.cpuHistory + it.toFloat()).takeLast(HISTORY_SIZE) } ?: current.cpuHistory,
          clusterHistory = clusterHistory,
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      if (hardware.value.snapshot == null) onUnavailable()
    } finally {
      mutableHardware.update { it.copy(loading = false) }
    }
  }

  private companion object { const val HISTORY_SIZE = 40 }
}
