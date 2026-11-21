# Repository Guidelines

## Project Structure

AndroidTweaker is a standalone Kotlin Android app with a Jetpack Compose Material 3 UI. The single Gradle module is `app/`. Under `app/src/main/java/com/c0d3h01/androidtweaker/`, `MainActivity.kt` hosts the UI and stable root Android components retain their installed identities. Feature packages are `battery/` (including analysis, monitor and ui), `charging/`, `access/`, `engine/`, `profiles/`, `tuning/`, `device/`, `settings/`, `home/`, and `app/` for navigation and coordination. Shared presentation lives in `ui/components/` and `ui/theme/`; small shared units/time functions live in `common/`. Read `docs/architecture/README.md` before adding a new source owner. Android resources live in `app/src/main/res/`. The APK bundles shell profiles and engine code from `app/src/main/assets/tweaker/`. JVM tests mirror feature packages under `app/src/test/java/`.

## Build, Test, and Run

Set `JAVA_HOME` to JDK 17 and `ANDROID_HOME` to an SDK with Android API 36 installed. Use the checked-in Gradle wrapper.

- `make doctor` checks the local toolchain.
- `make debug` builds the debug APK. Run `./gradlew :app:installDebug` with an ADB-connected device, then launch AndroidTweaker.
- `make check` runs JVM unit tests and checks the bundled scripts with `sh -n`.
- `make release` stages a signed APK at `app/release/androidtweaker.apk`. `make install` builds and installs that release.

## Coding Style

Use two-space indentation in Kotlin sources and shell scripts. Use PascalCase for Kotlin types and screen composables, camelCase for other functions and properties, and snake_case for Android resource names. Use Android-compatible `#!/system/bin/sh` for bundled scripts and quote path expansions. Keep user-facing strings in `res/values/strings.xml`.

## Testing

Use JUnit 4 and name test classes `*Test`, such as `ProfileTest`. Run the full JVM suite with `make test` or filter with `./gradlew :app:testDebugUnitTest --tests '*.ProfileTest'`. Add regression tests for behavior changes. No numeric coverage threshold is configured. For engine or boot changes, verify profile application, denied-root handling, and reboot reapplication on a rooted test device. Bump `TweakEngine.ENGINE_VERSION` when bundled scripts change so existing installs refresh their extracted copy.

## Commits and Pull Requests

Follow the Conventional Commit style used in recent history, such as `fix: handle denied root access` or `feat(app): add battery alerts`. In PRs, summarize the user-visible change, link relevant issues, list verification performed, and attach screenshots for UI changes.
