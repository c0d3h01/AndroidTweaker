# Usage, history, reports and data

## Usage page

The app shows screen-on/off, awake/deep-sleep statistics, drain estimates, foreground app usage, battery-care tips and an anomaly-alert option. Deep sleep is derived from elapsedRealtime minus uptimeMillis, accumulated as deltas; it is not a per-app wakelock attribution system. Completed discharge sessions supply screen statistics.

Drain rate filters non-charging samples by the selected screen state within six hours. It requires three points and at least 0.15 hours between first/last, rejects increasing level and caps results to 0–60%/h. The denominator spans the first and last matching timestamps, including intervening states; it is not an integration of only screen-on or only screen-off intervals. Samples mark charging by plugged state, whereas sessions distinguish active charging.

App usage requires Usage Access. Query begins at the last completed charging session end, or six hours ago. UsageStatsManager interval data are grouped by package; apps with at least 30 seconds foreground time are sorted, with at most eight returned. Share is foreground-time share, not battery-energy share. The “launches” field counts qualifying usage-stat records, not exact activity launches. Interval aggregation can extend beyond the intended session boundaries.

## History and storage

History shows a seven-day summary and session records; charging history also appears in Health. Summary totals completed sessions whose end timestamp is within the window: charge count, added mAh, average quality, discharge screen/deep-sleep time and level loss. Sessions crossing the window are counted whole, not prorated. Up to 400 sessions are retained; samples have a 24-hour/2,000-entry bound.

Battery settings, samples and sessions are serialized into the private `battery` SharedPreferences store with in-memory caches. Global tuning features use a separate preference store. Active session builders are not durably checkpointed.

## Export and deletion

CSV export writes completed sessions followed by samples to an app-cache file and shares it through FileProvider. The sample header currently omits the leading kind column present in sample rows; consumers must account for this inconsistency until fixed. JSON backup version 1 contains created timestamp, battery settings, samples and sessions. It does not include all tuning/profile preferences. No JSON restore/import flow is implemented.

Delete history requires confirmation and clears persisted/cached samples and sessions while retaining settings and alarms. It does not explicitly reset the monitor's in-memory session builder, so an active session may subsequently add history again. Exports leave the app only through the invoked Android share sheet; the app does not upload battery telemetry.

## Verification

Test empty history, retention boundaries, sessions spanning midnight/window boundaries, revoke Usage Access, CSV column alignment, JSON contents and deletion while monitoring. Proposed improvements belong in the roadmap rather than claims of existing behavior.

## Sources

[BatteryRepository.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/BatteryRepository.kt), [Battery.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/BatteryReader.kt), [BatteryScreen.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/BatteryScreen.kt).
