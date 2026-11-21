# Access modes and permissions

## Capability contract

| Capability | No privileged backend | Shizuku ADB (uid 2000) | Direct root / Shizuku root (uid 0) |
| --- | --- | --- | --- |
| Battery APIs, history, estimates, widget | Available; fields depend on device | Same public battery reader | Same reader; root does not automatically unlock its fields |
| App foreground usage | Requires separate Usage Access | Requires separate Usage Access | Requires separate Usage Access |
| Notifications | User/channel permission dependent | Same | Same |
| System settings/Doze/package commands | No privileged execution | Conditional shell commands | Conditional privileged commands |
| Apply full tuning profiles | Unavailable | UI does not grant full-profile capability | Available, device dependent |
| Kernel/sysfs tuning and charging holds | Unavailable | Unavailable | Requires supported writable nodes |
| Boot profile reapply | Unavailable | Not a persistent root substitute | Authorized direct-root boot flow |

Root is requested by an explicit connection action or authorized boot reapply. Reading ordinary device information must not prompt for root. ShellAccess prefers an active direct-root session; otherwise it uses the available Shizuku mode. Root-only calls require a backend whose capability flag permits profile application. Shizuku connection opens its manager when needed, requests authorization when running, and determines mode from the backend UID.

Usage Access is checked with AppOps and queried through UsageStatsManager. It does not provide per-app electrical consumption. On Android 13+, the UI requests notification permission when enabling the monitor/activity notifications. Notification channels can still be blocked independently.

## Failure behavior

Disconnected or denied root must leave public battery pages usable. Requested settings and successful device mutation are different states: a saved toggle alone is not evidence that an OEM accepted a shell command. Backend death, SELinux denial and missing commands/nodes must be handled as conditional availability.

## Verification

Test fresh install with every optional permission denied, Usage Access grant/revoke, notification grant/revoke, Shizuku absent/denied/ADB/root, direct-root denial and backend loss during a command. Confirm kernel actions never execute through ADB-only Shizuku.

## Sources

[ShizukuConnection.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/access/ShizukuConnection.kt), [RootConnection.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/access/RootConnection.kt), [Battery.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/BatteryReader.kt), [BootReceiver.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/BootReceiver.kt).
