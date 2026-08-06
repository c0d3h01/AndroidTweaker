# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.5.0] - 2026-06-14

### Changed

- Install banner now sources version from `module.prop` (single source of truth).
- Removed GitHub auto-launch on install; replaced with printed links.
- Boot wait in `service.sh` now polls up to 5 min then settles 15s (down from 30s fixed wait after up to 30s loop).
- Hardcoded `beastmode.profile` package name replaced with `companionPkg` field in `module.prop`, read by `service.sh` and `uninstall.sh`.

### Fixed

- `module.prop`: `description=null` literal replaced with empty string; `install.sh` populates it via `sed`.
- `uninstall.sh`: now uses `companionPkg` from `module.prop` instead of hardcoded package name.

## [1.4.9] - 2024-05-24

### Added

- Misc Tweaks.

### Changed

- Improved overall tweaks.
- Task Management.
- Improved priority functions.

### Fixed

- VM tweaks.
- Synchronisation.

### Removed

- Unnecessary tweaks.

## [1.4.7]

### Added

- New advanced management tweaks.
- Virtual memory tweaks.
- Zram tweaks.

### Changed

- Improved overall tweaks.
- Improved Services in execution.
- Improved VM tweaks.

### Fixed

- Compilation.
- BUSYBOX synchronisation.
- PID execution.

### Removed

- Unnecessary tweaks.

## [1.4.5]

### Added

- **New Tweaks:** Unlock new customization options for your device.
- **Cross-Device Sync:** Keep your preferences consistent across devices.

### Changed

- **Enhanced Functionality:** Enjoy smoother performance and improved responsiveness.
- **Simplified Installation:** Setup made easier for seamless usage.
- **Module Improvements:** Experience optimized performance with revamped modules.

## [1.4.1]

### Added

- Terminal menu.
- RAM cleaner on profile changing.

### Fixed

- General bugs.

### Removed

- APK due to incompatibility.
