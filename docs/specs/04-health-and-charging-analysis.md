# Health, capacity and charging analysis

## Sessions

The monitor builds charge and discharge sessions in memory. Charging or plugged/full readings start/continue a charge session. A plugged idle reading does not count as active charging; it updates an existing charge session without integrating charging current. Unplugging finishes the charge session and starts discharge tracking. Transitioning back to charging finishes discharge tracking. Sessions shorter than 45 seconds are discarded.

A session stores timestamps, start/end levels, added mAh, charger type, peak power/temperature, charge quality, estimated capacity, screen-on/off and deep-sleep time. Charge integration uses absolute current over elapsed time capped at 120 seconds per update. A positive charge-counter delta takes precedence over integrated current. Capacity extrapolation is added mAh / positive level gain × 100, requiring at least 15 percentage points; recorded estimates are bounded to 500–20,000 mAh.

## Health estimate

A usable charge session requires at least 15 percentage points gained, positive added mAh and at least five minutes duration. A plausible phone-reported full capacity takes precedence; when design capacity is known, full/design must fall within 0.40–1.30. Health percentage is capacity/design × 100, rounded and clamped to 0–130.

Without plausible phone capacity, the estimator uses usable session capacities within the same design-relative range. It takes the sorted middle value (upper middle for an even count). Confidence is Learning below two estimates, Approximate below four, Verified with at least four and spread ≤4%, otherwise Estimated. Phone capacity is labeled Verified after two usable sessions, otherwise Estimated. These labels describe the current heuristic, not laboratory validation.

Health labels: Excellent ≥90%, Good ≥80%, OK ≥70%, Fair ≥60%, Bad below 60%; missing percentage is “Not yet.” Missing design capacity can prevent a percentage even when a capacity is known. Cycle count is device-reported; the app does not synthesize it from sessions.

## Charge quality and speed

Quality starts at 100; subtract 1.5 per end-level point above 80%, 3 per peak °C above 35, and 0.4 per peak watt above 25. Charges gaining less than 20 points receive +4. Clamp/round to 0–100. Tiers are Gentle ≥80, Standard ≥55 and Stressful below 55. This is a heuristic score, not measured degradation.

Speed labels use either power or current: Super fast ≥15 W or ≥3000 mA, Fast ≥7 W or ≥1500 mA, Standard ≥2 W or ≥500 mA, otherwise Slow while plugged in. These do not identify a charging protocol.

Time-to-full uses remaining capacity/current; remaining discharge time first uses measured drain above 0.2%/h, then absolute current and capacity with a 20 mA minimum. Estimates can be absent and do not model charging taper or future workload changes.

## UI and verification

Health page includes capacity comparison, history chart and charge sessions. Verify learning/empty state, missing design/full capacity, outliers, counter fallback, charge holds and session transitions. Process death loses the in-memory active session; a capacity estimate must not imply continuous measurement through that gap.

## Sources

[Battery.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/BatteryReader.kt), [BatteryMonitorService.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/BatteryMonitorService.kt), [BatteryScreen.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/BatteryScreen.kt).
