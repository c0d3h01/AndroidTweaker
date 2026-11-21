# Device, settings and diagnostics

## Device overview

Device is a public-API-first information page with grouped device/platform, display, memory/storage and battery details, a share action and a link to live hardware. DeviceOverview and HardwareReader provide best-effort readable information. Missing/restricted values remain unavailable rather than triggering root authorization.

## Performance and hardware

Performance exposes profile selection/current and pending state, telemetry, CPU history, quick commands and links to tuning/hardware/access. Hardware shows platform, aggregate CPU and clusters, GPU, thermal zones, memory and battery details. Hardware/performance poll every four seconds only while the page is resumed; leaving them stops that UI polling. Kernel paths and GPU formats vary by device, so successful display on one device is not universal compatibility.

## Settings

Settings is the access hub: explicitly connect root, connect/open Shizuku, opt into profile reapplication at boot, and open Battery, Diagnostics or Logs. Boot reapply is conditional on saved profile and authorization policy. BootReceiver first attempts opted-in monitor startup, then starts the profile-apply foreground service if permitted. Boot support does not imply a Shizuku ADB server is automatically available after reboot.

## Diagnostics and logs

Diagnostics shows root/device details, engine and log locations, current profile, last apply result and refresh controls. Activity notifications are optional and separate from battery alarms/monitor notification. Logs reads the engine log and exposes the current access/retry-root flow. Apply failure is surfaced with captured output and a route to Logs; the UI must not silently promote a failed pending profile to confirmed success.

These pages support troubleshooting, not precision battery attribution or a benchmark certification. Engine output and root state do not prove every kernel setting took effect.

## Verification

Verify no-root Device loads, unavailable hardware fields, sharing, page visibility polling, failed apply output, root retry and boot opt-in/off. Check actual rooted reboot reapplication separately from JVM tests.

## Sources

[DeviceOverviewScreen.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/device/DeviceScreen.kt), [HardwareScreen.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/device/HardwareScreen.kt), [PerformanceScreen.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/device/PerformanceScreen.kt), [SettingsScreen.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/settings/SettingsScreen.kt), [DiagnosticsScreen.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/settings/DiagnosticsScreen.kt), [EngineLogScreen.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/settings/EngineLogScreen.kt).
