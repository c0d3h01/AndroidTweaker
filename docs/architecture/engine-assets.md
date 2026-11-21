# Bundled tweak engine

The engine has three source folders and one entry point. Each profile owns its actual tweak commands, so a contributor can read a mode from top to bottom without following a value table through separate modules.

```text
tweaker/
├── dispatcher.sh          # Load dependencies and route app commands
├── core/
│   ├── common.sh          # Safe writes, root/group gates, configuration, logging
│   ├── detect.sh          # CPU/GPU capabilities and frequency limits
│   └── notify.sh          # Notifications
├── profiles/
│   ├── battery.sh         # Direct battery-mode operations
│   ├── balanced.sh        # Direct balanced-mode operations
│   └── performance.sh     # Direct performance-mode operations
└── actions/
    ├── apps.sh            # App game-mode and package operations
    ├── charging.sh        # Monitor-owned charging holds and restoration
    ├── cleanup.sh         # RAM, cache, logs and runtime cleanup
    ├── display.sh         # Resolution and refresh-rate commands
    ├── doze.sh            # Idle whitelist and screen-off forced idle
    ├── maintenance.sh     # Storage trim and dexopt properties
    ├── network.sh         # Android network feature switches
    └── thermal.sh         # Explicit thermal-service switch
```

## Reading and editing a profile

The dispatcher detects hardware, sources the selected profile and calls its `apply_profile` function. That function loads saved overrides, then runs the actual node writes and Android commands in commented sections. There is no shared profile pipeline and no separate tuning implementation tree.

Each `if want cpu`, `if want gpu`, etc. preserves the app's group selection and root restrictions. Missing nodes are skipped by the shared `write` helper. Frequency limits come from hardware detection; CPU values use kHz and GPU values use Hz. Comments explain the mechanism, intended tradeoff and vendor limitations instead of promising universal improvements.

A write such as `write "${_clk}scaling_max_freq" "${CPU_MAX_FREQ-$CPU_HALF}"` directly shows the Battery mode default and retains the existing saved override. `${NAME-default}` is POSIX shell syntax: use NAME when set, otherwise use the inline default. An explicitly empty override remains empty where the old engine allowed that. These names remain compatible with the app's profile editor and `custom/*.conf` files.

The three files deliberately repeat their actual commands so each mode is readable on its own. A fix to shared hardware handling must be checked in all three profiles. Common I/O safety and capability detection remain in `core/` rather than being copied.

## Separate actions and runtime data

Global feature switches and one-shot commands live under `actions/`; profile-specific operations live in the profile itself. The existing `misc` command invokes `actions/maintenance.sh`. Command arguments, configuration names, charging restoration state and `tweaks.log` are preserved.

Root uses the extracted app-private tree. ADB Shizuku stages the same tree in a temporary directory. Engine version 8 refreshes installed scripts. Thermal-bypass requests in Performance are retained from the existing mode and can increase temperature; node availability and actual benefits remain device-specific.

## Verification

`make test-shell` checks syntax recursively, simulated charging, dispatcher loading from a path with spaces, and all three profiles against recorded pre-rewrite command traces. Profile traces cover defaults, saved overrides and disabled groups with representative simulated kernel nodes. They do not establish physical-device performance or root/Shizuku behavior. Run `make check` for JVM tests as well.
