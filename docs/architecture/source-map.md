# Source map

The current source root is `app/src/main/java/com/c0d3h01/androidtweaker/`. See the [architecture guide](README.md) for the reading path and dependency boundaries. The implementation plan retains the original names as a migration reference; this index lists current files.

## Android entry points

- [BatteryMonitorService.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/BatteryMonitorService.kt)
- [BatteryTileService.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/BatteryTileService.kt)
- [BatteryWidgetProvider.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/BatteryWidgetProvider.kt)
- [BootReceiver.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/BootReceiver.kt)
- [MainActivity.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/MainActivity.kt)
- [TweakApplyService.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/TweakApplyService.kt)

## app

- [AppDestination.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/app/AppDestination.kt)
- [AppScreen.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/app/AppScreen.kt)
- [AppState.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/app/AppState.kt)
- [AppViewModel.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/app/AppViewModel.kt)

## battery

- [BatteryExport.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/BatteryExport.kt)
- [BatteryFormatters.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/BatteryFormatters.kt)
- [BatteryJson.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/BatteryJson.kt)
- [BatteryModels.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/BatteryModels.kt)
- [BatteryReader.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/BatteryReader.kt)
- [BatteryRepository.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/BatteryRepository.kt)
- [BatteryScreen.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/BatteryScreen.kt)
- [BatterySettings.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/BatterySettings.kt)
- [BatteryUiState.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/BatteryUiState.kt)
- [BatteryViewModel.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/BatteryViewModel.kt)

## battery/analysis

- [ChargeQuality.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/analysis/ChargeQuality.kt)
- [DrainEstimator.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/analysis/DrainEstimator.kt)
- [HealthEstimator.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/analysis/HealthEstimator.kt)
- [ScreenTimeCalculator.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/analysis/ScreenTimeCalculator.kt)
- [UsageReader.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/analysis/UsageReader.kt)

## battery/monitor

- [BatteryAlertPolicy.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/monitor/BatteryAlertPolicy.kt)
- [BatteryNotifications.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/monitor/BatteryNotifications.kt)
- [BatteryReportPolicy.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/monitor/BatteryReportPolicy.kt)
- [MonitorPolicy.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/monitor/MonitorPolicy.kt)
- [SessionTracker.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/monitor/SessionTracker.kt)

## battery/ui

- [BatteryHealthTab.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/ui/BatteryHealthTab.kt)
- [BatteryHistoryTab.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/ui/BatteryHistoryTab.kt)
- [BatteryMonitorSettings.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/ui/BatteryMonitorSettings.kt)
- [BatteryOverviewTab.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/ui/BatteryOverviewTab.kt)
- [BatteryUsageTab.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/battery/ui/BatteryUsageTab.kt)

## charging

- [ChargingController.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/charging/ChargingController.kt)
- [ChargingControls.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/charging/ChargingControls.kt)
- [ChargingSettings.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/charging/ChargingSettings.kt)
- [ScreenOffAutomation.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/charging/ScreenOffAutomation.kt)

## access

- [AccessManager.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/access/AccessManager.kt)
- [AccessPresentation.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/access/AccessPresentation.kt)
- [AccessSettings.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/access/AccessSettings.kt)
- [CommandRunner.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/access/CommandRunner.kt)
- [RootConnection.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/access/RootConnection.kt)
- [RootInfo.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/access/RootInfo.kt)
- [RootProcess.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/access/RootProcess.kt)
- [RootReader.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/access/RootReader.kt)
- [ShizukuConnection.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/access/ShizukuConnection.kt)

## engine

- [EngineCommandPolicy.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/engine/EngineCommandPolicy.kt)
- [EngineCommands.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/engine/EngineCommands.kt)
- [EngineFiles.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/engine/EngineFiles.kt)
- [TweakEngine.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/engine/TweakEngine.kt)

## profiles

- [Profile.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/profiles/Profile.kt)
- [ProfileController.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/profiles/ProfileController.kt)
- [ProfileGroup.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/profiles/ProfileGroup.kt)
- [ProfilePreferences.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/profiles/ProfilePreferences.kt)
- [ProfilePresentation.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/profiles/ProfilePresentation.kt)
- [ProfileScreen.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/profiles/ProfileScreen.kt)
- [ProfileSettings.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/profiles/ProfileSettings.kt)
- [ProfileSettingsStore.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/profiles/ProfileSettingsStore.kt)
- [ProfileState.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/profiles/ProfileState.kt)

## tuning

- [AppProfileControls.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/tuning/AppProfileControls.kt)
- [DisplayControls.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/tuning/DisplayControls.kt)
- [FeatureSections.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/tuning/FeatureSections.kt)
- [NetworkControls.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/tuning/NetworkControls.kt)
- [PackagePicker.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/tuning/PackagePicker.kt)
- [PowerControls.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/tuning/PowerControls.kt)
- [SystemControls.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/tuning/SystemControls.kt)
- [ThermalControls.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/tuning/ThermalControls.kt)
- [TuningScreen.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/tuning/TuningScreen.kt)
- [TuningSettings.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/tuning/TuningSettings.kt)
- [TuningStore.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/tuning/TuningStore.kt)

## device

- [DeviceInfo.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/device/DeviceInfo.kt)
- [DeviceScreen.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/device/DeviceScreen.kt)
- [HardwareReader.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/device/HardwareReader.kt)
- [HardwareScreen.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/device/HardwareScreen.kt)
- [HardwareSnapshot.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/device/HardwareSnapshot.kt)
- [HardwareViewModel.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/device/HardwareViewModel.kt)
- [PerformancePanels.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/device/PerformancePanels.kt)
- [PerformanceScreen.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/device/PerformanceScreen.kt)

## settings

- [ActivityNotifications.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/settings/ActivityNotifications.kt)
- [DiagnosticsScreen.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/settings/DiagnosticsScreen.kt)
- [EngineLogScreen.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/settings/EngineLogScreen.kt)
- [LogLevel.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/settings/LogLevel.kt)
- [LogReader.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/settings/LogReader.kt)
- [SettingsScreen.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/settings/SettingsScreen.kt)

## home

- [OverviewScreen.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/home/OverviewScreen.kt)

## ui/components

- [Charts.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/ui/components/Charts.kt)
- [Controls.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/ui/components/Controls.kt)
- [MetricRow.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/ui/components/MetricRow.kt)
- [PageLayout.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/ui/components/PageLayout.kt)
- [Panel.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/ui/components/Panel.kt)
- [StatusChip.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/ui/components/StatusChip.kt)

## ui/theme

- [AppColors.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/ui/theme/AppColors.kt)
- [AppTheme.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/ui/theme/AppTheme.kt)
- [AppTypography.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/ui/theme/AppTypography.kt)

## common

- [SensorUnits.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/common/SensorUnits.kt)
- [TimeFormatters.kt](../../app/src/main/java/com/c0d3h01/androidtweaker/common/TimeFormatters.kt)
