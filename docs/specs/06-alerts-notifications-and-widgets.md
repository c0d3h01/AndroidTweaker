# Alerts, notifications and integrations

## Alarm settings

All alarms depend on a running monitor and OS notification/channel permission. Notification delivery is not guaranteed merely because the preference is enabled.

| Alarm | Default | Current trigger |
| --- | --- | --- |
| Full | On | FULL status or plugged level ≥100%; latched until reset |
| Low | On, 15% | Unplugged level at/below threshold; rising level resets latch |
| Chosen level | Off, 80% | Crossing upward while plugged or downward while unplugged; rearms after a three-point change |
| Hot | On, 45°C | At/above threshold; 30-minute cooldown |
| Cold | Off, 5°C | At/below threshold; 30-minute cooldown |
| Unusual drain | Off | Unplugged, screen off, calculated drain ≥8%/h; six-hour cooldown |

Stored threshold bounds: low level 2–50%, chosen level 5–100%, hot 35–70°C, cold −20–20°C. Alarm latches/cooldowns are process memory and can reset on service recreation.

## Reports

Charging summaries default on and are posted when a completed charge lasts at least two minutes and gains at least two points. Overnight reports default on and are evaluated during monitor activity between 06:00 and 10:59, once per stored day. Weekly reports default on and are evaluated Monday from 08:00, once per stored ISO week. These are opportunistic checks, not exact AlarmManager schedules; a stopped monitor or missing evaluation window can mean no report.

## Notification channels

The monitor uses a low-importance, silent foreground channel with live battery information and a pause action. Alarm notifications use a high-importance channel initially configured with vibration and the default alarm sound. Reports use a separate default-importance channel. Android/user channel settings control actual sound and visibility.

Status icon preferences include off, percentage, percentage + temperature and ring. Turning a custom icon style off does not eliminate the platform foreground-service obligation. Monitor state distinguishes selected opt-in from actual running status.

## Home widget and Quick Settings

The widget shows percentage/progress, state, temperature and power. The monitor pushes updates; normal AppWidget updates also read current battery data. Tapping opens Battery. It is not an independent high-frequency background recorder.

The tile listens for battery changes while visible and observes monitor state. Tap pauses a running/starting opted-in monitor or enables/starts monitoring otherwise. Its icon shows level; Android 10+ subtitle can show status and a conditional remaining/to-full estimate. Enabled-but-unavailable monitoring remains visibly different from running.

## Verification

Exercise each threshold crossing, rearm/cooldown, blocked channel, denied notifications, paused monitor, widget with unavailable fields and tile startup failure. Test report windows with controlled time in a test harness; do not assume exact-time delivery.

## Sources

[BatteryMonitorService.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/BatteryMonitorService.kt), [BatteryRepository.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/BatteryRepository.kt), [BatteryTileService.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/BatteryTileService.kt), [BatteryWidgetProvider.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/BatteryWidgetProvider.kt).
