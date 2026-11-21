# Root charging protection

## Current controls

These settings live in Tuning → Health & charging. They require direct root or root-backed Shizuku, supported power-supply nodes, and the running battery monitor. They are software controls, not firmware charge policies.

| Control | UI choices | Behavior |
| --- | --- | --- |
| Charge limit | Off, 60, 70, 80, 90, 100% | Hold at threshold, release at threshold minus two points |
| Charging bypass | Off/on | Hold battery charging; does not prove hardware direct-power bypass |
| Temperature fuse | Off, 40, 45, 50, 55°C | Hold at threshold, release at threshold minus 2°C |

Stored model permits charge limits 50–100 and fuse 35–60°C. The shell gives temperature fuse priority, then bypass, then charge limit. Missing temperature while a fuse hold is active preserves that hold; missing capacity during charge-limit evaluation returns without releasing it.

## Execution and restoration

ChargingController uses the existing root-capable runner; it does not independently prompt for root. It enforces on monitor ticks, normally at a minimum 20-second spacing with plug-edge handling. It can continue checking an owned hold even if disabling charging changes the reported plug state. Disabling all charging rules requests reset of an owned hold.

The charging script probes power-supply nodes including charging_enabled, charge_enabled, battery_charging_enabled, charging_enable, charge_disable, input_suspend and battery slate/input-suspend nodes. It accepts binary original values, records changed node/original/applied values in `custom/charge.restore`, and records the active reason separately. No supported changed control returns failure.

Reset only restores a node if its current value still matches the app-applied value, avoiding overwriting a later external change. Failed restorations remain pending for retry. Pause attempts release before service stop. Profile/feature application alone is not a standalone charging daemon.

## Limits and recovery

A write succeeding does not establish that the device physically stopped charging. OEM/driver compatibility requires physical testing. Force-stop, process death, lost root or reboot can interrupt enforcement/restoration. After an interrupted hold, reconnect root and start monitoring with charging rules disabled to request reset. There is no independent hardware failsafe or universal supported-device guarantee.

## Verification

Run the simulated shell charging test, then on a rooted device verify actual charge state/current, every priority transition, 2%/2°C hysteresis, unsupported nodes, denied writes, root loss, pause restoration, external node changes and interrupted-hold recovery. Never equate the bypass label with verified hardware passthrough.

## Sources

[ChargingController.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/charging/ChargingController.kt), [charging.sh](../../app/src/main/assets/tweaker/lib/tweaks/charging.sh), [FeatureSections.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/tuning/FeatureSections.kt), [charging_test.sh](../../app/src/test/shell/charging_test.sh).
