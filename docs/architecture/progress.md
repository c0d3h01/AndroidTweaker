# Source architecture implementation ledger

Baseline: JVM suite, debug assembly and lint passed; shell syntax and charging simulation passed on 2026-11-20. Existing Kotlin/Gradle deprecation warnings remain.

Ruling: work in the current checkout on refactor/battery-source-architecture, preserving the user's index — the current app is largely staged and differs substantially from HEAD; a clean HEAD worktree would omit the authorized source baseline. No automatic commits of the user's staged changes.

Ruling: retain current behavior during mechanical extraction, using existing behavior tests and compiler checks; add focused characterization tests at newly extracted logic seams rather than tests asserting filenames.

Pre-flight: tasks 2/8 share runner/mode contracts; keep stored mode names and root capability semantics. Tasks 3/4/5/6 share battery model/codec contracts; move symbols once and update imports atomically. Tasks 7/8 share config and execution serialization; keep one config writer and engine mutex. Tasks 2/9 share app state; hardware state must have one owner.

Physical device screenshots, charger compatibility and reboot verification are not available from baseline checks and remain manual validation requirements.

Task 1: baseline complete; compatibility inventory captured. Implementation snapshot (source only) stored under /private/tmp/androidtweaker-architecture-baseline for local comparisons.

## Implementation outcome

Source refactor implemented on `refactor/battery-source-architecture`. The initial implementation preserved existing index entries. The user subsequently requested grouped commits, a pull request, and squash integration into main; that integration is now explicitly authorized.

- Task 2: app navigation/destination/state and access packages extracted; public mode values and root authorization behavior retained.
- Task 3: battery models, Android reader, optional usage adapter, formatters and pure calculations separated. Shared temperature-unit normalization moved to common.
- Task 4: one BatteryRepository, separate BatterySettings/BatteryJson/BatteryExport; legacy settings/unknown fields/missing sensors/compact sample/session round trips covered by BatteryJsonTest. Added test-only org.json implementation so codec tests execute real JSON rather than Android stubs.
- Task 5: SessionTracker, BatteryAlertPolicy, BatteryReportPolicy and BatteryNotifications extracted. New session and alert tests first failed on missing extracted APIs, then passed. Clock values are supplied to the tracker; service supplies wall/monotonic/deep-sleep clocks, keeping policy tests independent of Android.
- Task 6: four battery tab panel files, monitor settings and dedicated BatteryViewModel/UiState. Visible polling remains lifecycle-cancelled.
- Task 7: ChargingSettings/ChargingController separated from ScreenOffAutomation. Existing feature preference keys and generated config names retained. Tuning controls grouped by purpose.
- Task 8: engine files and root-command policy separated; profiles have their own settings/group/store/editor/controller/state. Live pending/confirmed transition tests replace reliance on dead selection helpers.
- Task 9: hardware state has one feature owner; public device, performance, hardware, diagnostics and logs have descriptive names. Shared components/theme separated. Root metadata and log models moved out of hardware.
- Task 10: full source reference search confirmed BootProfile/TapArbiter/ProfileSelection had test-only callers; removed these helpers and their seven dead-helper tests. Removed empty Application and its manifest registration. UID validation retained next to CommandRunner. No bundled engine scripts removed or changed.
- Task 11: architecture reading guide/source map/contributor guide/current spec links/AGENTS paths updated; bundled-font notices inventoried from embedded name tables.

## Deliberate simplifications and deferred work

Ruling: retain small root Android components rather than add forwarding wrappers solely for package appearance — preserves widget/tile/intent identity and keeps entry files understandable.

Ruling: keep the serialized monitor event loop and foreground lifecycle together in the now much smaller root BatteryMonitorService — extracted state/notification policies remove the original size problem without another service coordinator.

Ruling: keep AccessManager.Mode nested; stored enum names and capability semantics remain unchanged. A separate enum file would add source churn without a distinct responsibility.

Ruling: keep shell config generation in the pure TuningSettings model and the single TuningStore write path — a separate FeatureConfigWriter would only forward the same operation. ChargingSettings is independently owned but uses the existing aggregate preference codec.

Ruling: no unused BatterySource interface or generic dependency container — current reader is concrete and calculation/session seams already support deterministic tests. Add substitution interfaces when an actual second reader or injectable Android test requires them.

Ruling: current profile taps while applying are ignored, not queued last-tap-wins. Preserve the live baseline policy and test it; old TapArbiter tests did not represent production behavior.

Ruling: hardcoded-string/resource regrouping is deferred to a dedicated localization change. Resource IDs/values and visible text were preserved here; do not combine a large translation rewrite with package moves.

Ruling (initial implementation): leave changes uncommitted while preserving the user's staged baseline. Superseded by the explicit request for grouped commits: the saved pre-refactor source snapshot now forms a separately validated app commit, followed by the architecture refactor, CI, documentation, README, funding cleanup, and rebuilt artifact commits.

## Review and verification

Independent read-only review compared current source to `/private/tmp/androidtweaker-architecture-baseline/app/src`, not the substantially older HEAD. No Critical/Important behavior regression found. Corrected the accidentally renamed visible Features label; added the missing codec compatibility coverage and corrected assertion imports found during follow-up checks.

Verified: 71 JVM tests, zero failures/errors/skips; debug and release assembly; debug lint; shell syntax and simulated charging policy. Bundled-engine assets are byte-identical to baseline, so ENGINE_VERSION remains 5. Documentation links and whitespace are checked separately after documentation edits.

Emulator smoke check: installed the final debug build, opened Battery and Device through their existing launch destination strings, confirmed the app remained running, and captured unedited screenshots under docs/screenshots. This does not establish physical-device charging or upgrade compatibility.

Still manual/unverified: physical no-root/privileged device flows, widget/tile upgrade behavior, supported-device charging read-back/hold/recovery, rooted reboot reapplication, monitoring overhead measurements. Public distribution also needs full upstream font license files/provenance and resolved dependency notice review; this refactor does not publish the app or certify release readiness.
