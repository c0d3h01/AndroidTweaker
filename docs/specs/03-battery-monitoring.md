# Live battery monitoring

## Implemented measurements

The Battery live page shows charge level/state, charger type, current, voltage, temperature, power and time estimates, plus charging and temperature panels. BatteryReader uses BatteryManager and the sticky battery broadcast, then directly readable battery/bms sysfs files. It does not execute root commands to read restricted capacity files.

| Value | Source and interpretation |
| --- | --- |
| Level | BatteryManager capacity, falling back to broadcast level/scale |
| State | Charging, discharging, full, idle or unknown from broadcast |
| Connection | AC, USB, wireless, dock or none; plugged is distinct from actively charging |
| Current | CURRENT_NOW microamps converted to rounded mA; sysfs fallback |
| Voltage | Broadcast mV, otherwise sysfs microvolts divided by 1000 |
| Temperature | Broadcast tenths of °C, otherwise normalized sysfs value |
| Power | Absolute current mA × voltage mV / 1,000,000; battery-side estimate |
| Charge counter | Positive µAh reading, otherwise readable sysfs |
| Design/full capacity, cycles, health | Device-exposed fields; absent values remain unavailable |
| Screen state | PowerManager interactive state |

Power is not charger wall-input power or negotiated USB-PD power. Driver current signs and units can vary. Unknown values must not be presented as measured zeros.

## Monitor lifecycle

Monitoring defaults off. Start persists opt-in and starts BatteryMonitorService as a foreground service; the screen requests notification permission. State distinguishes STOPPED, STARTING, RUNNING and UNAVAILABLE. The service observes battery, power and screen broadcasts plus settings changes. Pause persists disabled state; a running service attempts charging-hold release before stopping. BootReceiver attempts to restart an opted-in monitor.

The visible Battery screen refreshes readings every 2.5 seconds while resumed, even when historical monitoring is paused. This is distinct from service sampling and history persistence.

## Adaptive service timing

MonitorPolicy clamps active refresh to 15–60 seconds despite the legacy stored default of 5 seconds and stored range 2–60. With plugged-in charging control, screen-on polling is at most 20 seconds and screen-off polling is 20 seconds. Otherwise screen-on uses the active delay, screen-off charging uses 30 seconds, idle sampling when needed uses five minutes, and no timer is scheduled when none is needed. Broadcasts can trigger earlier work. These coroutine delays are not a guarantee of exact wakeups during device suspend.

History normally records at roughly 55-second spacing, with state edges allowed sooner. BatteryRepository further deduplicates unchanged charging/screen states less than 20 seconds apart. Samples are retained for 24 hours and capped at 2,000.

## Verification and gaps

Verify screen-on/off, charger insertion/removal, paused live viewing, missing fields and failed foreground-service startup. Measure overhead on a physical device. Force-stop and process death interrupt collection; current code must not be described as gap-free recording. Polling interval labels and effective timing need reconciliation.

## Sources

[Battery.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/BatteryReader.kt), [MonitorPolicy.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/monitor/MonitorPolicy.kt), [BatteryMonitorService.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/BatteryMonitorService.kt), [BatteryRepository.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/BatteryRepository.kt).
