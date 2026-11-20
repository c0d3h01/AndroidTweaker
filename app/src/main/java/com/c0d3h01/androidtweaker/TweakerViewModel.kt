package com.c0d3h01.androidtweaker

import android.app.Application
import android.os.UserManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.c0d3h01.androidtweaker.data.FeatureStore
import com.c0d3h01.androidtweaker.data.Features
import com.c0d3h01.androidtweaker.data.Profile
import com.c0d3h01.androidtweaker.data.ProfilePreferences
import com.c0d3h01.androidtweaker.data.RootInfo
import com.c0d3h01.androidtweaker.data.RootReader
import com.c0d3h01.androidtweaker.data.SystemReader
import com.c0d3h01.androidtweaker.data.TelemetrySnapshot
import com.c0d3h01.androidtweaker.data.TweakEngine
import com.c0d3h01.androidtweaker.data.key
import com.c0d3h01.androidtweaker.util.ShellAccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class TweakerState(
  val profileCode: Int,
  val profileAccess: ShellAccess.Mode?,
  val features: Features,
  val reapplyOnBoot: Boolean,
  val activityNotifications: Boolean,
  val notificationsAllowed: Boolean,
  val access: ShellAccess.Mode = ShellAccess.Mode.NONE,
  val root: RootInfo? = null,
  val connecting: Boolean = false,
  val applying: Boolean = false,
  val pendingCode: Int? = null,
  val lastApply: TweakEngine.Result? = null,
  val showApplyFailure: Boolean = false,
  val message: String? = null,
)

data class HardwareState(
  val snapshot: TelemetrySnapshot? = null,
  val cpuHistory: List<Float> = emptyList(),
  val clusterHistory: Map<String, List<Float>> = emptyMap(),
  val loading: Boolean = false,
)

class TweakerViewModel(application: Application) : AndroidViewModel(application) {
  private val context = application
  private val mutations = Mutex()
  private var featuresJob: Job? = null
  private val mutableState = MutableStateFlow(
    TweakerState(
      profileCode = ProfilePreferences.currentCode(context),
      profileAccess = ProfilePreferences.lastProfileAccess(context),
      features = FeatureStore.load(context),
      reapplyOnBoot = ProfilePreferences.reapplyOnBoot(context),
      activityNotifications = ActivityNotifier.enabled(context),
      notificationsAllowed = ActivityNotifier.channelAllowed(context),
    ),
  )
  private val mutableHardware = MutableStateFlow(HardwareState())
  val state = mutableState.asStateFlow()
  val hardware = mutableHardware.asStateFlow()
  val logPath = TweakEngine.logFile(context).absolutePath
  val enginePath = TweakEngine.directory(context).absolutePath

  fun refreshAccess(details: Boolean = false) {
    viewModelScope.launch {
      val access = withContext(Dispatchers.IO) { ShellAccess.mode() }
      mutableState.update {
        it.copy(access = access, notificationsAllowed = ActivityNotifier.channelAllowed(context))
      }
      if (details && !state.value.connecting) {
        val root = withContext(Dispatchers.IO) { RootReader.read() }
        if (!state.value.connecting) mutableState.update { it.copy(root = root) }
      }
    }
  }

  fun connectRoot() {
    if (state.value.connecting) return
    mutableState.update { it.copy(connecting = true) }
    viewModelScope.launch {
      try {
        val (root, access) = withContext(Dispatchers.IO) {
          RootReader.read(requestRoot = true) to ShellAccess.mode()
        }
        mutableState.update {
          it.copy(
            root = root,
            access = access,
            message = context.getString(if (root.granted) R.string.root_connected else R.string.root_unavailable),
          )
        }
      } finally {
        mutableState.update { it.copy(connecting = false) }
      }
    }
  }

  fun reportShizukuAccess(granted: Boolean) {
    showMessage(if (granted) R.string.shizuku_connected else R.string.shizuku_unavailable)
    refreshAccess()
  }

  fun setBootReapply(enabled: Boolean) {
    if (enabled && state.value.profileAccess != ShellAccess.Mode.ROOT) return
    ProfilePreferences.setReapplyOnBoot(context, enabled)
    mutableState.update { it.copy(reapplyOnBoot = enabled) }
  }

  fun setActivityNotifications(enabled: Boolean) {
    ActivityNotifier.setEnabled(context, enabled)
    mutableState.update { it.copy(activityNotifications = enabled) }
  }

  fun clearMessage(message: String) {
    mutableState.update { if (it.message == message) it.copy(message = null) else it }
  }

  fun dismissApplyFailure() {
    mutableState.update { it.copy(showApplyFailure = false) }
  }

  fun refreshHardware() {
    viewModelScope.launch { sampleHardware() }
  }

  suspend fun sampleHardware() {
    if (hardware.value.loading) return
    mutableHardware.update { it.copy(loading = true) }
    try {
      val sample = SystemReader.read(context)
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
      if (hardware.value.snapshot == null) showMessage(R.string.hardware_unavailable)
    } finally {
      mutableHardware.update { it.copy(loading = false) }
    }
  }

  fun applyProfile(profile: Profile) {
    if (state.value.applying) return
    if (!state.value.access.canApplyProfiles) {
      showMessage(R.string.profile_access_required)
      return
    }
    mutableState.update { it.copy(applying = true, pendingCode = profile.code) }
    viewModelScope.launch {
      try {
        val (result, mode) = mutations.withLock {
          withContext(Dispatchers.IO) {
            var access = ShellAccess.Mode.NONE
            val result = withEngine {
              val runner = ShellAccess.ensure(requireRoot = true)
              access = ShellAccess.modeOf(runner)
              TweakEngine.apply(context, runner, profile)
            }
            result to access
          }
        }
        mutableState.update {
          it.copy(
            profileCode = if (result.ok) profile.code else it.profileCode,
            profileAccess = if (result.ok) mode else it.profileAccess,
            lastApply = result,
            showApplyFailure = !result.ok,
          )
        }
        if (result.ok) {
          ActivityNotifier.profileApplied(context, profile)
          showMessage(R.string.profile_applied)
        }
        refreshAccess()
      } finally {
        mutableState.update { it.copy(applying = false, pendingCode = null) }
      }
    }
  }

  fun saveFeatures(next: Features) {
    val clean = next.sanitized()
    mutableState.update { it.copy(features = clean) }
    FeatureStore.save(context, clean)
    if (state.value.access == ShellAccess.Mode.NONE) return
    featuresJob?.cancel()
    featuresJob = viewModelScope.launch {
      delay(300)
      val result = mutations.withLock {
        withContext(Dispatchers.IO) {
          withEngine { TweakEngine.command(context, ShellAccess.ensure(), "feature") }
        }
      }
      if (!result.ok) showMessage(R.string.feature_apply_failed)
      refreshAccess()
    }
  }

  fun runCommand(args: String) {
    if (state.value.applying) return
    if (state.value.access == ShellAccess.Mode.NONE) {
      showMessage(R.string.command_access_required)
      return
    }
    if (TweakEngine.commandRequiresRoot(args) && !state.value.access.canApplyProfiles) {
      showMessage(R.string.command_root_required)
      return
    }
    if ((args == "bloat" || args.startsWith("restore ")) &&
      context.getSystemService(UserManager::class.java)?.isSystemUser != true
    ) {
      showMessage(R.string.command_primary_user_required)
      return
    }
    mutableState.update { it.copy(applying = true) }
    viewModelScope.launch {
      try {
        val result = mutations.withLock {
          withContext(Dispatchers.IO) {
            withEngine { TweakEngine.command(context, ShellAccess.ensure(), args) }
          }
        }
        showMessage(if (result.ok) R.string.command_complete else R.string.command_failed)
        refreshAccess()
      } finally {
        mutableState.update { it.copy(applying = false) }
      }
    }
  }

  private fun showMessage(message: Int) {
    mutableState.update { it.copy(message = context.getString(message)) }
  }

  private suspend fun withEngine(block: suspend () -> TweakEngine.Result): TweakEngine.Result =
    try {
      block()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      TweakEngine.Result(ok = false, exitCode = -1, output = e.message.orEmpty(), logPath = logPath)
    }

  private companion object {
    const val HISTORY_SIZE = 40
  }
}
