package com.c0d3h01.androidtweaker.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.c0d3h01.androidtweaker.R
import com.c0d3h01.androidtweaker.ScreenRequest
import com.c0d3h01.androidtweaker.access.ShizukuConnection
import com.c0d3h01.androidtweaker.battery.BatteryScreen
import com.c0d3h01.androidtweaker.battery.BatteryViewModel
import com.c0d3h01.androidtweaker.device.DeviceScreen
import com.c0d3h01.androidtweaker.device.HardwareScreen
import com.c0d3h01.androidtweaker.device.HardwareViewModel
import com.c0d3h01.androidtweaker.device.PerformanceScreen
import com.c0d3h01.androidtweaker.home.OverviewScreen
import com.c0d3h01.androidtweaker.settings.DiagnosticsScreen
import com.c0d3h01.androidtweaker.settings.EngineLogScreen
import com.c0d3h01.androidtweaker.settings.SettingsScreen
import com.c0d3h01.androidtweaker.tuning.TuningScreen
import com.c0d3h01.androidtweaker.ui.theme.AppColors
import kotlinx.coroutines.delay

@Composable
fun AppScreen(
  model: AppViewModel,
  batteryModel: BatteryViewModel,
  hardwareModel: HardwareViewModel,
  screenRequest: ScreenRequest?,
) {
  val context = LocalContext.current
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  val state by model.state.collectAsStateWithLifecycle()
  val snackbar = remember { SnackbarHostState() }
  val history = rememberSaveableStateHolder()
  var pageKey by rememberSaveable { mutableStateOf(AppDestination.fromKey(screenRequest?.screen)?.key ?: AppDestination.OVERVIEW.key) }
  val page = AppDestination.fromKey(pageKey) ?: AppDestination.OVERVIEW

  fun open(destination: AppDestination) {
    pageKey = destination.key
  }

  val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
    model.refreshAccess()
  }

  fun requestNotifications() {
    if (Build.VERSION.SDK_INT >= 33 &&
      context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
      permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  fun connectShizuku() {
    if (ShizukuConnection.running()) ShizukuConnection.request(model::reportShizukuAccess)
    else ShizukuConnection.openManager(context)
  }

  LaunchedEffect(screenRequest) {
    AppDestination.fromKey(screenRequest?.screen)?.let(::open)
  }
  BackHandler(page != AppDestination.OVERVIEW) {
    open(if (page != page.section) page.section else AppDestination.OVERVIEW)
  }
  LaunchedEffect(lifecycle) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { model.refreshAccess() }
  }
  LaunchedEffect(lifecycle, page) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
      when (page) {
        AppDestination.HARDWARE, AppDestination.PERFORMANCE -> {
          if (page == AppDestination.PERFORMANCE) model.refreshAccess(details = true)
          while (true) {
            hardwareModel.sampleHardware(model::reportHardwareUnavailable)
            delay(4_000)
          }
        }
        AppDestination.TOOLS -> {
          model.refreshAccess(details = true)
          hardwareModel.sampleHardware(model::reportHardwareUnavailable)
        }
        AppDestination.LOGS -> model.refreshAccess(details = true)
        else -> Unit
      }
    }
  }
  LaunchedEffect(state.message) {
    state.message?.let { message ->
      snackbar.showSnackbar(message)
      model.clearMessage(message)
    }
  }

  val screen: @Composable () -> Unit = {
    history.SaveableStateProvider(page.key) {
      when (page) {
        AppDestination.OVERVIEW -> OverviewScreen(
          state = state,
          onBattery = { open(AppDestination.BATTERY) },
          onPerformance = { open(AppDestination.PERFORMANCE) },
        )
        AppDestination.BATTERY -> BatteryScreen(onRequestNotifications = ::requestNotifications, model = batteryModel)
        AppDestination.DEVICE -> DeviceScreen(onOpenHardware = { open(AppDestination.HARDWARE) })
        AppDestination.PERFORMANCE -> {
          val hardware by hardwareModel.hardware.collectAsStateWithLifecycle()
          PerformanceScreen(
            snapshot = hardware.snapshot,
            root = state.root,
            currentCode = state.profileCode,
            pendingCode = state.pendingCode,
            loading = hardware.loading && hardware.snapshot == null,
            cpuHistory = hardware.cpuHistory,
            onRefresh = { hardwareModel.refreshHardware(model::reportHardwareUnavailable) },
            onSelect = model::applyProfile,
            onOpenHardware = { open(AppDestination.HARDWARE) },
            onOpenTweaks = { open(AppDestination.TUNING) },
            onOpenAccess = { open(AppDestination.SETTINGS) },
            shell = state.access,
            quickBusy = state.applying,
            onQuickCommand = model::runCommand,
          )
        }
        AppDestination.TUNING -> TuningScreen(
          currentCode = state.profileCode,
          pendingCode = state.pendingCode,
          features = state.features,
          shell = state.access,
          onApply = model::applyProfile,
          onFeatures = model::saveFeatures,
          onCommand = model::runCommand,
          onGrantShizuku = ::connectShizuku,
          onOpenShizuku = { ShizukuConnection.openManager(context) },
          onOpenAccess = { open(AppDestination.SETTINGS) },
          busy = state.applying,
        )
        AppDestination.SETTINGS -> SettingsScreen(
          state = state,
          onConnectRoot = model::connectRoot,
          onConnectShizuku = ::connectShizuku,
          onBootReapply = model::setBootReapply,
          onDiagnostics = { open(AppDestination.TOOLS) },
          onLogs = { open(AppDestination.LOGS) },
          onBattery = { open(AppDestination.BATTERY) },
        )
        AppDestination.HARDWARE -> {
          val hardware by hardwareModel.hardware.collectAsStateWithLifecycle()
          HardwareScreen(
            snapshot = hardware.snapshot,
            loading = hardware.loading && hardware.snapshot == null,
            cpuHistory = hardware.cpuHistory,
            clusterHistory = hardware.clusterHistory,
            onRefresh = { hardwareModel.refreshHardware(model::reportHardwareUnavailable) },
          )
        }
        AppDestination.LOGS -> EngineLogScreen(
          root = state.root,
          logPath = model.logPath,
          onRetryRoot = model::connectRoot,
        )
        AppDestination.TOOLS -> {
          val hardware by hardwareModel.hardware.collectAsStateWithLifecycle()
          DiagnosticsScreen(
            snapshot = hardware.snapshot,
            root = state.root,
            currentCode = state.profileCode,
            lastApply = state.lastApply,
            enginePath = model.enginePath,
            logPath = model.logPath,
            loading = hardware.loading || state.connecting,
            activityNotifications = state.activityNotifications,
            notificationsAllowed = state.notificationsAllowed,
            onActivityNotificationsChange = { enabled ->
              model.setActivityNotifications(enabled)
              if (enabled) requestNotifications()
            },
            onRefresh = {
              model.refreshAccess(details = true)
              hardwareModel.refreshHardware(model::reportHardwareUnavailable)
            },
            onRetryRoot = model::connectRoot,
          )
        }
      }
    }
  }

  BoxWithConstraints(
    Modifier.fillMaxSize().background(AppColors.Canvas).safeDrawingPadding().imePadding(),
  ) {
    if (maxWidth >= 700.dp) {
      Row(Modifier.fillMaxSize()) {
        NavigationRail(
          containerColor = AppColors.Panel,
          windowInsets = WindowInsets(0, 0, 0, 0),
        ) {
          AppDestination.sections.forEach { target ->
            NavigationRailItem(
              selected = page.section == target,
              onClick = { open(target) },
              icon = { Icon(checkNotNull(target.icon), contentDescription = null) },
              label = { Text(stringResource(target.title), maxLines = 1, overflow = TextOverflow.Ellipsis) },
              colors = NavigationRailItemDefaults.colors(
                indicatorColor = AppColors.SignalSoft,
                selectedIconColor = AppColors.Signal,
                selectedTextColor = AppColors.Signal,
                unselectedIconColor = AppColors.InkMuted,
                unselectedTextColor = AppColors.InkMuted,
              ),
            )
          }
        }
        ScreenFrame(Modifier.weight(1f), snackbar, screen)
      }
    } else {
      Column(Modifier.fillMaxSize()) {
        ScreenFrame(Modifier.weight(1f), snackbar, screen)
        HorizontalDivider(color = AppColors.Hairline)
        NavigationBar(containerColor = AppColors.Panel, windowInsets = WindowInsets(0, 0, 0, 0)) {
          AppDestination.sections.forEach { target ->
            NavigationBarItem(
              selected = page.section == target,
              onClick = { open(target) },
              icon = { Icon(checkNotNull(target.icon), contentDescription = null) },
              label = { Text(stringResource(target.title), maxLines = 1, overflow = TextOverflow.Ellipsis) },
              colors = NavigationBarItemDefaults.colors(
                indicatorColor = AppColors.SignalSoft,
                selectedIconColor = AppColors.Signal,
                selectedTextColor = AppColors.Signal,
                unselectedIconColor = AppColors.InkMuted,
                unselectedTextColor = AppColors.InkMuted,
              ),
            )
          }
        }
      }
    }
  }

  if (state.showApplyFailure) {
    AlertDialog(
      onDismissRequest = model::dismissApplyFailure,
      title = { Text(stringResource(R.string.profile_apply_failed)) },
      text = {
        Text(state.lastApply?.output?.trim()?.takeLast(500)?.ifBlank { null }
          ?: stringResource(R.string.profile_apply_failed_detail))
      },
      confirmButton = {
        TextButton(onClick = model::dismissApplyFailure) { Text(stringResource(android.R.string.ok)) }
      },
      dismissButton = {
        TextButton(onClick = {
          model.dismissApplyFailure()
          open(AppDestination.LOGS)
        }) { Text(stringResource(R.string.nav_logs)) }
      },
    )
  }
}

@Composable
private fun ScreenFrame(
  modifier: Modifier,
  snackbar: SnackbarHostState,
  content: @Composable () -> Unit,
) {
  Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
    Box(Modifier.widthIn(max = 960.dp).fillMaxSize()) {
      content()
      SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp))
    }
  }
}
