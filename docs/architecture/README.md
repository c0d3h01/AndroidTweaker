# Source architecture

AndroidTweaker uses one Android app module. Code is grouped by the feature a contributor wants to change. Kotlin packages are under `app/src/main/java/com/c0d3h01/androidtweaker/`; tests mirror those packages under `app/src/test/java/`.

## Start reading here

1. [MainActivity](../../app/src/main/java/com/c0d3h01/androidtweaker/MainActivity.kt) creates screen models and handles existing launch destinations.
2. [AppScreen](../../app/src/main/java/com/c0d3h01/androidtweaker/app/AppScreen.kt) composes navigation; [AppDestination](../../app/src/main/java/com/c0d3h01/androidtweaker/app/AppDestination.kt) defines the sections.
3. [BatteryScreen](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/BatteryScreen.kt) routes the four battery tabs. [BatteryViewModel](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/BatteryViewModel.kt) owns visible-page reads and actions.
4. [BatteryMonitorService](../../app/src/main/java/com/c0d3h01/androidtweaker/BatteryMonitorService.kt) owns background lifecycle. Its policies, sessions, reports and notifications live in `battery/monitor/`.
5. [BatteryRepository](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/BatteryRepository.kt) owns existing local settings/history; `BatteryJson` preserves its format and `BatteryExport` creates share files.
6. [ChargingController](../../app/src/main/java/com/c0d3h01/androidtweaker/charging/ChargingController.kt) handles optional root charging holds. [TweakEngine](../../app/src/main/java/com/c0d3h01/androidtweaker/engine/TweakEngine.kt) serializes privileged commands through [AccessManager](../../app/src/main/java/com/c0d3h01/androidtweaker/access/AccessManager.kt).

## Ownership

| Package | Owns |
| --- | --- |
| `app` | Navigation, app state and cross-feature coordination |
| `battery` | Readings/models, screen state, settings/history codec, export and formatting |
| `battery/analysis` | Health, drain, screen-time calculations and the optional UsageStats adapter |
| `battery/monitor` | Session state machine, alarm/report policies, notification delivery and cadence |
| `battery/ui` | Live, health, usage and history panels; monitor/alert settings |
| `charging` | Charging preferences/UI, root hold/release and separate screen-off automation |
| `access` | Root/Shizuku transport, capability selection, connection UI and access metadata |
| `engine` | Serialized dispatcher invocation, command policy, extraction/runtime files |
| `profiles` | Named profiles, per-profile settings, display metadata, editor and apply state/controller |
| `tuning` | Global setting persistence/configuration and controls grouped by purpose |
| `device` | Public device overview, privileged telemetry and visible-only hardware state |
| `settings` | Settings, diagnostics, logs and activity notifications |
| `home` | Entry summary and feature shortcuts |
| `ui/components`, `ui/theme` | Reusable presentation, colors and typography |
| `common` | Small shared sensor-unit/time functions |

## Follow a battery reading

Android battery APIs/readable files → `BatteryReader` → `BatteryMonitorService` → `SessionTracker`/`BatteryAlertPolicy` → `BatteryRepository` → battery tabs. `BatteryNotifications` renders policy decisions; `BatteryReportPolicy` checks opportunistic report windows. Visible-page refresh is lifecycle-cancelled and does not start an additional historical recorder.

Follow a tuning action: feature control → `ProfileController` → compatible preferences/configuration → `TweakEngine` mutex → `EngineCommandPolicy` and `AccessManager` → root/Shizuku → bundled dispatcher. Requested settings and successful physical read-back are different; this refactor does not add capability verification.

## Boundaries

Keep pure calculations free of Android/Compose and privileged backends. Keep Android reads and notification rendering in their named adapters. Screens render state and invoke feature actions; shell execution belongs to the engine. Do not add general `utils`, `helpers` or technical-layer dumping grounds.

`TuningStore` deliberately retains the existing preference layout. `TuningSettings` combines a feature-owned `ChargingSettings` value with other global switches and renders the same shell configuration. The code has one config writer, not separate stores that could race. `BatteryReader` remains a concrete object; no unused source interface was added.

Small root Android entry classes remain where they were for compatibility. In particular, widget/tile class identity is retained rather than creating wrappers just to move small files. The empty custom Application was removed. Service lifecycle remains in the root monitor service; extracted policies make a second generic monitor coordinator unnecessary.

## Compatibility and follow-ups

See [preserved contracts](compatibility.md), [implementation ledger](progress.md), [source map](source-map.md) and [feature specs](../specs/README.md). Crash-safe sessions, CSV correction, backup restore, cadence/UI reconciliation and verified charging capability are separate behavior projects, not hidden refactor changes. Remaining hardcoded UI text requires a dedicated localization pass; resource IDs and existing string XML are preserved in this migration.
