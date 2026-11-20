package com.c0d3h01.androidtweaker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.c0d3h01.androidtweaker.ui.BatteryScreen
import com.c0d3h01.androidtweaker.ui.Bench
import com.c0d3h01.androidtweaker.ui.DashboardScreen
import com.c0d3h01.androidtweaker.ui.DeviceOverviewScreen
import com.c0d3h01.androidtweaker.ui.HardwareScreen
import com.c0d3h01.androidtweaker.ui.LogsScreen
import com.c0d3h01.androidtweaker.ui.OverviewScreen
import com.c0d3h01.androidtweaker.ui.SettingsScreen
import com.c0d3h01.androidtweaker.ui.ToolsScreen
import com.c0d3h01.androidtweaker.ui.TweaksScreen
import com.c0d3h01.androidtweaker.util.ShizukuAccess
import kotlinx.coroutines.delay

private enum class Page(
  val key: String,
  @StringRes val title: Int,
  val icon: ImageVector? = null,
) {
  OVERVIEW(MainActivity.SCREEN_HOME, R.string.nav_overview, Icons.Outlined.Dashboard),
  BATTERY(MainActivity.SCREEN_BATTERY, R.string.nav_battery, Icons.Outlined.BatteryChargingFull),
  DEVICE(MainActivity.SCREEN_DEVICE, R.string.nav_device, Icons.Outlined.Devices),
  TUNING(MainActivity.SCREEN_TWEAKS, R.string.nav_tuning, Icons.Outlined.Tune),
  SETTINGS(MainActivity.SCREEN_SETTINGS, R.string.nav_settings, Icons.Outlined.Settings),
  PERFORMANCE(MainActivity.SCREEN_PERFORMANCE, R.string.nav_performance),
  HARDWARE(MainActivity.SCREEN_HARDWARE, R.string.nav_hardware),
  LOGS(MainActivity.SCREEN_LOGS, R.string.nav_logs),
  TOOLS(MainActivity.SCREEN_TOOLS, R.string.nav_diagnostics),
  ;

  val section: Page
    get() = when (this) {
      HARDWARE, PERFORMANCE -> DEVICE
      LOGS, TOOLS -> SETTINGS
      else -> this
    }

  companion object {
    val sections = entries.filter { it.icon != null }

    fun fromKey(key: String?) = entries.firstOrNull { it.key == key }
  }
}

@Composable
fun TweakerApp(
  model: TweakerViewModel,
  screenRequest: ScreenRequest?,
) {
  val context = LocalContext.current
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  val state by model.state.collectAsStateWithLifecycle()
  val snackbar = remember { SnackbarHostState() }
  val history = rememberSaveableStateHolder()
  var pageKey by rememberSaveable { mutableStateOf(Page.fromKey(screenRequest?.screen)?.key ?: Page.OVERVIEW.key) }
  val page = Page.fromKey(pageKey) ?: Page.OVERVIEW

  fun open(destination: Page) {
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
    if (ShizukuAccess.running()) ShizukuAccess.request(model::reportShizukuAccess)
    else ShizukuAccess.openManager(context)
  }

  LaunchedEffect(screenRequest) {
    Page.fromKey(screenRequest?.screen)?.let(::open)
  }
  BackHandler(page != Page.OVERVIEW) {
    open(if (page != page.section) page.section else Page.OVERVIEW)
  }
  LaunchedEffect(lifecycle) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { model.refreshAccess() }
  }
  LaunchedEffect(lifecycle, page) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
      when (page) {
        Page.HARDWARE, Page.PERFORMANCE -> {
          if (page == Page.PERFORMANCE) model.refreshAccess(details = true)
          while (true) {
            model.sampleHardware()
            delay(4_000)
          }
        }
        Page.TOOLS -> {
          model.refreshAccess(details = true)
          model.sampleHardware()
        }
        Page.LOGS -> model.refreshAccess(details = true)
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
        Page.OVERVIEW -> OverviewScreen(
          state = state,
          onBattery = { open(Page.BATTERY) },
          onPerformance = { open(Page.PERFORMANCE) },
        )
        Page.BATTERY -> BatteryScreen(onRequestNotifications = ::requestNotifications)
        Page.DEVICE -> DeviceOverviewScreen(onOpenHardware = { open(Page.HARDWARE) })
        Page.PERFORMANCE -> {
          val hardware by model.hardware.collectAsStateWithLifecycle()
          DashboardScreen(
            snapshot = hardware.snapshot,
            root = state.root,
            currentCode = state.profileCode,
            pendingCode = state.pendingCode,
            loading = hardware.loading && hardware.snapshot == null,
            cpuHistory = hardware.cpuHistory,
            onRefresh = model::refreshHardware,
            onSelect = model::applyProfile,
            onOpenHardware = { open(Page.HARDWARE) },
            onOpenTweaks = { open(Page.TUNING) },
            onOpenAccess = { open(Page.SETTINGS) },
            shell = state.access,
            quickBusy = state.applying,
            onQuickCommand = model::runCommand,
          )
        }
        Page.TUNING -> TweaksScreen(
          currentCode = state.profileCode,
          pendingCode = state.pendingCode,
          features = state.features,
          shell = state.access,
          onApply = model::applyProfile,
          onFeatures = model::saveFeatures,
          onCommand = model::runCommand,
          onGrantShizuku = ::connectShizuku,
          onOpenShizuku = { ShizukuAccess.openManager(context) },
          onOpenAccess = { open(Page.SETTINGS) },
          busy = state.applying,
        )
        Page.SETTINGS -> SettingsScreen(
          state = state,
          onConnectRoot = model::connectRoot,
          onConnectShizuku = ::connectShizuku,
          onBootReapply = model::setBootReapply,
          onDiagnostics = { open(Page.TOOLS) },
          onLogs = { open(Page.LOGS) },
          onBattery = { open(Page.BATTERY) },
        )
        Page.HARDWARE -> {
          val hardware by model.hardware.collectAsStateWithLifecycle()
          HardwareScreen(
            snapshot = hardware.snapshot,
            loading = hardware.loading && hardware.snapshot == null,
            cpuHistory = hardware.cpuHistory,
            clusterHistory = hardware.clusterHistory,
            onRefresh = model::refreshHardware,
          )
        }
        Page.LOGS -> LogsScreen(
          root = state.root,
          logPath = model.logPath,
          onRetryRoot = model::connectRoot,
        )
        Page.TOOLS -> {
          val hardware by model.hardware.collectAsStateWithLifecycle()
          ToolsScreen(
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
              model.refreshHardware()
            },
            onRetryRoot = model::connectRoot,
          )
        }
      }
    }
  }

  BoxWithConstraints(
    Modifier.fillMaxSize().background(Bench.Canvas).safeDrawingPadding().imePadding(),
  ) {
    if (maxWidth >= 700.dp) {
      Row(Modifier.fillMaxSize()) {
        NavigationRail(
          containerColor = Bench.Panel,
          windowInsets = WindowInsets(0, 0, 0, 0),
        ) {
          Page.sections.forEach { target ->
            NavigationRailItem(
              selected = page.section == target,
              onClick = { open(target) },
              icon = { Icon(checkNotNull(target.icon), contentDescription = null) },
              label = { Text(stringResource(target.title), maxLines = 1, overflow = TextOverflow.Ellipsis) },
              colors = NavigationRailItemDefaults.colors(
                indicatorColor = Bench.SignalSoft,
                selectedIconColor = Bench.Signal,
                selectedTextColor = Bench.Signal,
                unselectedIconColor = Bench.InkMuted,
                unselectedTextColor = Bench.InkMuted,
              ),
            )
          }
        }
        ScreenFrame(Modifier.weight(1f), snackbar, screen)
      }
    } else {
      Column(Modifier.fillMaxSize()) {
        ScreenFrame(Modifier.weight(1f), snackbar, screen)
        HorizontalDivider(color = Bench.Hairline)
        NavigationBar(containerColor = Bench.Panel, windowInsets = WindowInsets(0, 0, 0, 0)) {
          Page.sections.forEach { target ->
            NavigationBarItem(
              selected = page.section == target,
              onClick = { open(target) },
              icon = { Icon(checkNotNull(target.icon), contentDescription = null) },
              label = { Text(stringResource(target.title), maxLines = 1, overflow = TextOverflow.Ellipsis) },
              colors = NavigationBarItemDefaults.colors(
                indicatorColor = Bench.SignalSoft,
                selectedIconColor = Bench.Signal,
                selectedTextColor = Bench.Signal,
                unselectedIconColor = Bench.InkMuted,
                unselectedTextColor = Bench.InkMuted,
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
          open(Page.LOGS)
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
