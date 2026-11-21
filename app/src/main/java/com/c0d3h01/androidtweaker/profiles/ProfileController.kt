package com.c0d3h01.androidtweaker.profiles

import android.content.Context
import android.os.UserManager
import com.c0d3h01.androidtweaker.R
import com.c0d3h01.androidtweaker.access.AccessManager
import com.c0d3h01.androidtweaker.engine.EngineCommandPolicy
import com.c0d3h01.androidtweaker.engine.EngineFiles
import com.c0d3h01.androidtweaker.engine.TweakEngine
import com.c0d3h01.androidtweaker.settings.ActivityNotifications
import com.c0d3h01.androidtweaker.tuning.TuningSettings
import com.c0d3h01.androidtweaker.tuning.TuningStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
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

/** All tuning mutations share one gate; taps while applying are ignored, as before. */
class ProfileController(
  private val context: Context,
  private val scope: CoroutineScope,
  private val currentAccess: () -> AccessManager.Mode,
  private val reportMessage: (Int) -> Unit,
  private val onAccessRefresh: () -> Unit,
) {
  private val mutations = Mutex()
  private var featuresJob: Job? = null
  private val mutableState = MutableStateFlow(ProfileState(
    profileCode = ProfilePreferences.currentCode(context),
    profileAccess = ProfilePreferences.lastProfileAccess(context),
    features = TuningStore.load(context),
  ))
  val state = mutableState.asStateFlow()
  private val logPath = EngineFiles.logFile(context).absolutePath

  fun dismissApplyFailure() { mutableState.update { it.copy(showApplyFailure = false) } }

  fun applyProfile(profile: Profile) {
    if (state.value.applying) return
    if (!currentAccess().canApplyProfiles) {
      reportMessage(R.string.profile_access_required)
      return
    }
    mutableState.update { it.beginApply(profile.code) ?: it }
    scope.launch {
      try {
        val (result, mode) = mutations.withLock {
          withContext(Dispatchers.IO) {
            var access = AccessManager.Mode.NONE
            val result = withEngine {
              val runner = AccessManager.ensure(requireRoot = true)
              access = AccessManager.modeOf(runner)
              TweakEngine.apply(context, runner, profile)
            }
            result to access
          }
        }
        mutableState.update {
          it.completeApply(profile.code, mode, result)
        }
        if (result.ok) {
          ActivityNotifications.profileApplied(context, profile)
          reportMessage(R.string.profile_applied)
        }
        onAccessRefresh()
      } finally {
        mutableState.update { it.copy(applying = false, pendingCode = null) }
      }
    }
  }

  fun saveFeatures(next: TuningSettings) {
    val clean = next.sanitized()
    mutableState.update { it.copy(features = clean) }
    TuningStore.save(context, clean)
    if (currentAccess() == AccessManager.Mode.NONE) return
    featuresJob?.cancel()
    featuresJob = scope.launch {
      delay(300)
      val result = mutations.withLock {
        withContext(Dispatchers.IO) {
          withEngine { TweakEngine.command(context, AccessManager.ensure(), "feature") }
        }
      }
      if (!result.ok) reportMessage(R.string.feature_apply_failed)
      onAccessRefresh()
    }
  }

  fun runCommand(args: String) {
    if (state.value.applying) return
    if (currentAccess() == AccessManager.Mode.NONE) {
      reportMessage(R.string.command_access_required)
      return
    }
    if (EngineCommandPolicy.commandRequiresRoot(args) && !currentAccess().canApplyProfiles) {
      reportMessage(R.string.command_root_required)
      return
    }
    if ((args == "bloat" || args.startsWith("restore ")) &&
      context.getSystemService(UserManager::class.java)?.isSystemUser != true
    ) {
      reportMessage(R.string.command_primary_user_required)
      return
    }
    mutableState.update { it.copy(applying = true) }
    scope.launch {
      try {
        val result = mutations.withLock {
          withContext(Dispatchers.IO) {
            withEngine { TweakEngine.command(context, AccessManager.ensure(), args) }
          }
        }
        reportMessage(if (result.ok) R.string.command_complete else R.string.command_failed)
        onAccessRefresh()
      } finally {
        mutableState.update { it.copy(applying = false) }
      }
    }
  }

  private suspend fun withEngine(block: suspend () -> TweakEngine.Result): TweakEngine.Result =
    try {
      block()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      TweakEngine.Result(ok = false, exitCode = -1, output = e.message.orEmpty(), logPath = logPath)
    }

}
