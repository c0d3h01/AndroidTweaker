package com.c0d3h01.androidtweaker.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.c0d3h01.androidtweaker.R
import com.c0d3h01.androidtweaker.access.AccessManager
import com.c0d3h01.androidtweaker.access.RootReader
import com.c0d3h01.androidtweaker.engine.EngineFiles
import com.c0d3h01.androidtweaker.profiles.Profile
import com.c0d3h01.androidtweaker.profiles.ProfileController
import com.c0d3h01.androidtweaker.profiles.ProfilePreferences
import com.c0d3h01.androidtweaker.settings.ActivityNotifications
import com.c0d3h01.androidtweaker.tuning.TuningSettings
import com.c0d3h01.androidtweaker.tuning.TuningStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: Application) : AndroidViewModel(application) {
  private val context = application
  private val mutableState = MutableStateFlow(
    AppState(
      profileCode = ProfilePreferences.currentCode(context),
      profileAccess = ProfilePreferences.lastProfileAccess(context),
      features = TuningStore.load(context),
      reapplyOnBoot = ProfilePreferences.reapplyOnBoot(context),
      activityNotifications = ActivityNotifications.enabled(context),
      notificationsAllowed = ActivityNotifications.channelAllowed(context),
    ),
  )
  private val profiles = ProfileController(
    context, viewModelScope,
    currentAccess = { mutableState.value.access },
    reportMessage = ::showMessage,
    onAccessRefresh = { refreshAccess() },
  )
  val state = combine(mutableState, profiles.state) { app, profile ->
    app.copy(
      profileCode = profile.profileCode,
      profileAccess = profile.profileAccess,
      features = profile.features,
      applying = profile.applying,
      pendingCode = profile.pendingCode,
      lastApply = profile.lastApply,
      showApplyFailure = profile.showApplyFailure,
    )
  }.stateIn(viewModelScope, SharingStarted.Eagerly, mutableState.value)
  val logPath = EngineFiles.logFile(context).absolutePath
  val enginePath = EngineFiles.directory(context).absolutePath

  fun refreshAccess(details: Boolean = false) {
    viewModelScope.launch {
      val access = withContext(Dispatchers.IO) { AccessManager.mode() }
      mutableState.update {
        it.copy(access = access, notificationsAllowed = ActivityNotifications.channelAllowed(context))
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
          RootReader.read(requestRoot = true) to AccessManager.mode()
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
    if (enabled && state.value.profileAccess != AccessManager.Mode.ROOT) return
    ProfilePreferences.setReapplyOnBoot(context, enabled)
    mutableState.update { it.copy(reapplyOnBoot = enabled) }
  }

  fun setActivityNotifications(enabled: Boolean) {
    ActivityNotifications.setEnabled(context, enabled)
    mutableState.update { it.copy(activityNotifications = enabled) }
  }

  fun clearMessage(message: String) {
    mutableState.update { if (it.message == message) it.copy(message = null) else it }
  }

  fun dismissApplyFailure() {
    profiles.dismissApplyFailure()
  }

  fun applyProfile(profile: Profile) = profiles.applyProfile(profile)

  fun saveFeatures(next: TuningSettings) = profiles.saveFeatures(next)

  fun runCommand(args: String) = profiles.runCommand(args)

  fun reportHardwareUnavailable() = showMessage(R.string.hardware_unavailable)

  private fun showMessage(message: Int) {
    mutableState.update { it.copy(message = context.getString(message)) }
  }

}
