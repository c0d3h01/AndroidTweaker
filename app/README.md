# AndroidTweaker App

The `app` module builds a Kotlin and Jetpack Compose Material 3 APK. It supports Android 8.0 (API 26) and later and targets Android 16 (API 36).

## Architecture

Read the [source architecture guide](../docs/architecture/README.md) for package ownership and a contributor walkthrough.

- `MainActivity` hosts `app/AppScreen`; `app/AppViewModel` coordinates access and app-wide messages. `profiles/ProfileController` owns serialized tuning mutations, while `device/HardwareViewModel` owns visible-only hardware state.
- `battery/BatteryViewModel` owns visible-page refresh and actions. `BatteryMonitorService` owns opted-in background collection and delegates to `battery/monitor` for sessions, alarms, reports and notification rendering.
- `battery/BatteryRepository` retains local settings/history; `BatteryJson` preserves existing serialized formats and `BatteryExport` creates local share files. Pure health/drain calculations live in `battery/analysis`.
- `access/RootConnection` and `access/ShizukuConnection` provide optional privilege. Public device/battery reads do not request root. ADB-backed Shizuku remains limited to shell-level operations.
- `engine/TweakEngine` serializes bundled dispatcher commands. `charging/ChargingController` owns charge holds/restoration; `charging/ScreenOffAutomation` owns screen transitions. Runtime paths and stored keys remain compatible.

Battery records, settings, and diagnostic logs stay on the device. Exports use the Android share sheet. App usage is available through the optional Android Usage Access grant.

Software charging holds require a running monitor and a root connection. Pause attempts to restore the original values of the nodes AndroidTweaker changed. If Android force-stops the app during a hold, reconnect root and restart monitoring with charging rules disabled to restore those values.

## Release signing

`./gradlew :app:assembleRelease` creates the optimized APK. By default, release builds use the shared development key configured in `app/release.properties`. `make release` stages the signed APK. To use your own signing key, override these values in the ignored root `local.properties`:

```properties
KEYSTORE_FILE=/path/to/your-release.keystore
KEYSTORE_PASSWORD=your-store-password
KEY_ALIAS=androidtweaker
KEY_PASSWORD=your-key-password
```

`make release` writes `app/release/androidtweaker.apk`. Use a private key for distribution. The historical shared development key is unsuitable for public releases. In-place Android upgrades require the same package ID and a compatible signing certificate.

## Verification

Run `make check` for JVM tests, shell syntax, and the simulated charging policy. On stock Android, verify that the Device screen loads without a privilege prompt. Enable battery monitoring, background the app, and check the notification and Pause action. For privileged changes, test on a device with the relevant backend and verify the resulting system setting. Check reboot reapply separately with direct root.
