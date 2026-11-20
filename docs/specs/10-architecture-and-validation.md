# Architecture, persistence and validation

## Ownership and data flow

Single Kotlin Android module with Compose Material 3. MainActivity hosts AppScreen; AppViewModel owns access/profile/feature state and serializes user tuning operations. BatteryScreen owns visible battery-page state and reads shared BatteryRepository data. BatteryMonitorService owns background session builders, alarms, report checks, widget updates and ChargingController ticks.

BatteryManager/broadcast/readable sysfs → BatteryReading → foreground monitor → samples/completed sessions → BatteryRepository → battery UI, health/drain estimates and exports. Separately, UI feature/profile changes → preferences/generated configuration → TweakEngine → ShellAccess runner → extracted dispatcher → device settings/nodes and log output.

There is no cloud battery analysis service, standalone root daemon or Magisk module requirement. The public battery reader and privileged hardware/tuning pipeline are distinct; connecting root does not rewrite BatteryReader's data-source policy.

## Persisted state

BatteryRepository persists battery settings plus JSON samples/sessions. TuningStore persists global toggles/package lists and generates shell configuration. ProfilePreferences/ProfileSettingsStore retain profile selection, boot behavior and per-profile overrides. Engine assets are versioned; bundled-script changes require an ENGINE_VERSION bump so installed copies refresh. In-memory service sessions and enforcement flags are not durable event logs.

## Build and release

Use JDK 17 and Android SDK API 36 with the Gradle wrapper. `make doctor` checks tools; `make check` runs JVM tests and shell checks, including the charging simulation. `make release` stages a signed APK at `app/release/androidtweaker.apk`, using shared development signing properties with local overrides. Distribution requires an appropriate private signing identity; upgrades require certificate compatibility. Refer to the app developer guide for signing values, not this document.

## Required validation matrix

| Scenario | Evidence needed |
| --- | --- |
| Stock non-root, no optional grants | Battery/device usable, no root prompt, unavailable fields honest |
| Notifications denied/channel blocked | Clear monitor state, no claim that alerts will be delivered |
| Usage Access revoked | Empty/permission state; no fabricated per-app drain |
| Root denied or Shizuku dies | Public features remain usable; command failure is visible |
| Charging rules on supported rooted device | Physical read-back, actual hold/release, hysteresis and restoration |
| No supported charging node | Unsupported/failure result; no successful-protection claim |
| Screen off / unplug / charge hold | Correct session transitions and bounded sampling overhead |
| Process death / force-stop / reboot | Explicit data gaps and interrupted-hold recovery |
| Export / delete while active | Parseable files, documented scope and session behavior |
| Multiple OEMs and API levels | Device-specific results recorded, not inferred from emulator success |

Existing JVM tests cover battery calculations, monitor policy, features, profile/command handling, device parsing and shell-access logic. Shell charging tests simulate node behavior. Neither demonstrates physical charger compatibility. No physical-device verification was performed as part of writing these specs.

## Sources

[AppViewModel.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/app/AppViewModel.kt), [TweakEngine.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/engine/TweakEngine.kt), [JVM tests](../../app/src/test/java/com/c0d3h01/androidtweaker), [Makefile](../../Makefile), [app developer guide](../../app/README.md).
