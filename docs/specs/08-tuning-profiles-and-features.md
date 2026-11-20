# Tuning profiles and global features

## Profiles and selection

Battery, Balanced, Performance and Gaming have stable codes 1–4. Full profile application requires a root-capable backend. Each profile combines bundled shell values with saved group selection and optional CPU/GPU ceiling, swappiness and read-ahead overrides. Overrides are written before apply; ceilings are relative to the device maximum. The engine detects available hardware paths and dispatches enabled groups. A profile name is an intended tradeoff, not measured proof of longer life or higher performance.

Presets are All, Recommended and None. Current Recommended excludes the Thermal group only, even though a source comment also mentions KSM. Disabling a group skips future writes; it does not guarantee restoration of previous mutations.

| Section | Implemented groups |
| --- | --- |
| Core | CPU governor/clocks/boost, GPU limits/idle, kernel/cgroups/wakelocks, scheduler, stune boost, storage I/O |
| Memory/storage | VM swappiness/dirty/writeback, filesystem/inotify/leases, KSM |
| Power | Thermal/workqueues, Doze timings |
| Network | TCP/socket settings |
| Display | Wake/touch/power hints |
| System | Process priorities and system-service settings |

## Global feature inventory

Global features persist separately from profiles and are mirrored to `custom/features.conf`. Fresh defaults disable optional switches. They are applied after profiles and through feature commands; some rules additionally need monitor screen transitions.

| Feature | How it works / conditions |
| --- | --- |
| Charge limit, bypass, temperature fuse | Monitor-enforced root controls; see charging spec |
| Disable throttling | Root thermal-service/trip-point changes; conflicts with a conservative battery-care goal |
| Force Doze | Monitor enters idle on screen-off and unforces on screen-on; shell-capable backend |
| Doze whitelist | Selected package list passed to idle feature commands |
| Kill background apps | `am kill-all` on observed screen-off transition; no guaranteed energy benefit |
| Auto power save | Enables system low-power setting on screen-off, disables when returning if app marked it applied |
| Auto data saver | Enables background-data restriction on screen-off, reverses app-marked application on screen-on |
| Reduce debug logging | Cleanup/system logging commands, with backend-dependent effectiveness |
| Phantom process limit | Stored 4–64, UI Off/8/16/32/64; system command support varies |
| Wi-Fi sleep and scan-always switches | Network settings commands; OEM/Android support varies |
| Heavy/light app lists | Per-package game-mode rules; package cannot belong to both, light wins sanitization |
| Bloatware selection | Explicit removal action for user 0; restore command uses package reinstall-for-user support |
| Resolution | Native, 75%, 16:9 through window-manager size commands |
| Peak refresh rate | Stored 30–240 Hz or 0/default; requested setting does not establish panel capability |
| Free RAM / clear cache | Root-only UI commands |
| Clear logs / recompile ART | Shell commands, backend/platform dependent |

Package identifiers are sanitized before generated configuration. Removal has a confirmation flow. Reinstallation for a user does not promise restoration of deleted app data. Per-app configuration is not a background foreground-app profile automation engine.

## Engine behavior and gaps

Assets are extracted into app storage and invoked through `lib/dispatcher.sh`; there is no standalone daemon/property watcher. ADB Shizuku can execute eligible settings/cmd/pm/dumpsys operations but kernel groups are skipped. Full-profile UI capability remains root-only. Command parsing uses an exit marker and captures output. A successful outer dispatcher exit is not a per-setting read-back verification; scripts can skip unsupported nodes or suppress command output.

Screen-transition automation keeps applied flags in process memory. It does not maintain a durable snapshot of every preexisting device setting. Reverting a toggle is not necessarily a full original-state rollback.

## Verification

Verify each profile and enabled group against actual read-back, missing nodes and denied writes. Verify package validation, destructive-action confirmation, ADB-only restrictions, settings transitions and restart behavior. Use logs to distinguish attempted, skipped and failed work.

## Sources

[TuningSettings.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/tuning/TuningSettings.kt), [TweakGroups.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/profiles/ProfileGroup.kt), [ChargingController.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/charging/ChargingController.kt), [dispatcher.sh](../../app/src/main/assets/tweaker/lib/dispatcher.sh), [profiles](../../app/src/main/assets/tweaker/profiles).
