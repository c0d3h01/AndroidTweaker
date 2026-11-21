# Compatibility contracts

The refactor changes source organization, not installed-app identity or persisted formats.

- Package/application ID: `com.c0d3h01.androidtweaker`; SDK 26 minimum, 36 target/compile.
- Keep root MainActivity, BootReceiver, BatteryTileService and BatteryWidgetProvider registrations. Keep intent destination values home/battery/device/settings/performance/tweaks/hardware/logs/tools and extra `com.c0d3h01.androidtweaker.extra.SCREEN`.
- Keep monitor start/stop actions `com.c0d3h01.androidtweaker.BATTERY_MONITOR_START` / `BATTERY_MONITOR_STOP`, boot action `com.c0d3h01.androidtweaker.BOOT_APPLY` and boot extra `profile_code`.
- Battery preference file `battery`: settings/samples/sessions JSON and overnight_day/weekly_week report markers. Keep compact JSON keys and optional-field behavior.
- Feature preference file `feature_switches`; preserve every existing switch/package-list key. Per-profile store `tweak_groups`; profile store `profile` with profile/profile_access/reapply_on_boot.
- Profile codes Battery=1, Balanced=2, Performance=3, Gaming=4. Access modes ROOT, SHIZUKU_ROOT, SHIZUKU, NONE retain their persisted names.
- Keep generated features.conf/profile configuration keys and existing engine asset/runtime root `tweaker`, `.engine-5`, `tweaks.log`, `custom/charge.restore` and `custom/charge.reason`. Root checks and command exit marker `__AT_EXIT:` remain.
- Monitor notification ID 43; boot apply ID 41; monitor/alarm/report channels battery_monitor/battery_alarms/battery_reports; boot channel boot_apply. Alarm/report ID bases 500/600.
- FileProvider `${applicationId}.fileprovider`, Shizuku provider `${applicationId}.shizuku`; retain grants and paths.
- CSV and backup JSON version 1 remain unchanged, including the documented existing CSV sample-header inconsistency. Correct it separately with regression coverage.
- Preserve development-key fallback and local signing overrides. No credential values belong in this inventory. Distribution upgrades require the existing intended certificate.

Mechanical migration must not change monitor cadence, alarm defaults, health math, feature defaults or root prompts. Existing widget instances and notification actions require device upgrade checks.
