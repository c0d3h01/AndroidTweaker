# Product and page structure

## Current experience

The app combines a battery suite with privileged device tuning. The five main destinations are Overview, Battery, Device, Tuning and Settings. The battery suite is usable without connecting root.

| Page | Current role | Related spec |
| --- | --- | --- |
| Overview | Entry summary and shortcuts to battery and performance | Battery monitoring and device specs |
| Battery: Overview tab | Gauge, metrics, charging analysis, temperature chart, alarms and monitor settings | 03, 04, 06 |
| Battery: Health tab | Health estimate, capacity history and charge sessions | 04 |
| Battery: Usage tab | Screen use, drain, app foreground usage, care tips and anomaly option | 05 |
| Battery: History tab | Seven-day summary, session history, CSV, JSON backup and delete | 05 |
| Device | Public device overview, share and hardware entry | 09 |
| Performance | Profile selection, telemetry and quick commands | 08, 09 |
| Hardware | CPU/cluster, GPU, thermal, memory and battery details | 09 |
| Tuning | Per-profile groups/overrides and global feature controls | 08 |
| Settings | Root/Shizuku connection, boot reapply and links to battery/tools/logs | 02, 09 |
| Diagnostics | Access and engine details, last apply and activity notifications | 09 |
| Logs | Engine log inspection | 09 |

Performance and Hardware belong to the Device navigation section; Diagnostics and Logs belong to Settings. Back returns from a child to its section, then to Overview. Launch requests can open a particular page, including Battery from the widget. Page state uses saveable Compose state. At widths of at least 700 dp the app uses a navigation rail; smaller windows use bottom navigation. Content is constrained to 960 dp.

## Current constraints

Battery controls are split between Battery (monitoring/alarms) and Tuning (charging holds). Health/history primarily use completed sessions. Some battery UI text remains hardcoded rather than stored in Android string resources. These are current implementation details to address in the proposed roadmap.

## Verification

Open all five main pages without granting root; navigate into each child and back. Confirm the widget opens Battery, state survives ordinary navigation, and wide layouts retain access to all destinations.

## Sources

[AppScreen.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/app/AppScreen.kt), [BatteryScreen.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/BatteryScreen.kt), [MainActivity.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/MainActivity.kt).
