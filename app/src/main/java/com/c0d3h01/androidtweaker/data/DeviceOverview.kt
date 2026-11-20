package com.c0d3h01.androidtweaker.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.StatFs
import android.view.Display
import kotlin.math.roundToInt

internal data class DeviceOverview(
  val capturedAt: Long,
  val model: String?,
  val manufacturer: String?,
  val androidVersion: String?,
  val apiLevel: Int,
  val securityPatch: String?,
  val storage: DeviceCapacity?,
  val memory: DeviceCapacity?,
  val batteryPercent: Int?,
  val batteryStatus: Int?,
  val powerSaver: Boolean?,
  val thermalStatus: Int?,
  val displayMode: Display.Mode?,
  val network: DeviceNetwork?,
)

internal data class DeviceCapacity(val available: Long, val total: Long)

internal data class DeviceNetwork(
  val internet: DeviceInternet,
  val transport: DeviceTransport? = null,
  val metered: Boolean? = null,
)

internal enum class DeviceInternet { DISCONNECTED, CAPTIVE_PORTAL, VALIDATED, UNVALIDATED, LOCAL_ONLY }

internal enum class DeviceTransport { VPN, WI_FI, CELLULAR, ETHERNET, BLUETOOTH, OTHER }

/** Reads Android framework state without invoking a shell, network requests, or privileged APIs. */
internal fun readDeviceOverview(context: Context, display: Display?): DeviceOverview {
  val storage = try {
    val stats = StatFs(context.dataDir.path)
    deviceCapacity(stats.availableBytes, stats.totalBytes)
  } catch (_: IllegalArgumentException) {
    null
  }
  val memory = context.getSystemService(ActivityManager::class.java)?.let { manager ->
    val info = ActivityManager.MemoryInfo()
    manager.getMemoryInfo(info)
    deviceCapacity(info.availMem, info.totalMem)
  }
  val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    ?.takeIf { it.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true) }
  val level = battery?.let {
    batteryPercentage(it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1), it.getIntExtra(BatteryManager.EXTRA_SCALE, 0))
  }
  val power = context.getSystemService(PowerManager::class.java)
  val thermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) power?.currentThermalStatus else null
  val activeDisplay = display ?: context.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)
  return DeviceOverview(
    capturedAt = System.currentTimeMillis(),
    model = Build.MODEL.reportedValue(),
    manufacturer = Build.MANUFACTURER.reportedValue(),
    androidVersion = Build.VERSION.RELEASE.reportedValue(),
    apiLevel = Build.VERSION.SDK_INT,
    securityPatch = Build.VERSION.SECURITY_PATCH.reportedValue(),
    storage = storage,
    memory = memory,
    batteryPercent = level,
    batteryStatus = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN),
    powerSaver = power?.isPowerSaveMode,
    thermalStatus = thermal,
    displayMode = activeDisplay?.takeIf { it.isValid }?.mode,
    network = readDeviceNetwork(context),
  )
}

private fun readDeviceNetwork(context: Context): DeviceNetwork? {
  val manager = context.getSystemService(ConnectivityManager::class.java) ?: return null
  val capabilities = try {
    val network = manager.activeNetwork ?: return DeviceNetwork(DeviceInternet.DISCONNECTED)
    manager.getNetworkCapabilities(network) ?: return null
  } catch (_: SecurityException) {
    return null
  }
  val internet = when {
    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) -> DeviceInternet.CAPTIVE_PORTAL
    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> DeviceInternet.VALIDATED
    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> DeviceInternet.UNVALIDATED
    else -> DeviceInternet.LOCAL_ONLY
  }
  val transport = when {
    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> DeviceTransport.VPN
    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> DeviceTransport.WI_FI
    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> DeviceTransport.CELLULAR
    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> DeviceTransport.ETHERNET
    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> DeviceTransport.BLUETOOTH
    else -> DeviceTransport.OTHER
  }
  return DeviceNetwork(
    internet,
    transport,
    metered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
  )
}

internal fun batteryPercentage(level: Int, scale: Int): Int? =
  if (scale > 0 && level in 0..scale) (level * 100.0 / scale).roundToInt() else null

internal fun deviceCapacity(available: Long, total: Long): DeviceCapacity? =
  if (total > 0 && available in 0..total) DeviceCapacity(available, total) else null

private fun String?.reportedValue(): String? =
  this?.trim()?.takeUnless { it.isBlank() || it.equals(Build.UNKNOWN, ignoreCase = true) }
