# Proposed battery-first product roadmap

Everything in this document is proposed direction unless explicitly identified as current. It is not an implementation commitment or a shipped-feature list.

## Goal and principles

Make the non-root experience complete for observing battery behavior and making informed choices. Offer root controls as an optional, capability-tested extension. Advanced means useful measurements with known provenance, working controls with verified outcomes, and recovery when the app or backend stops.

Retain device/tuning tools as supporting diagnostics. Prefer battery monitoring, health, charging and drain workflows in primary navigation. Do not treat cleanup, disabled thermal protections or aggressive background killing as automatically beneficial battery optimizations.

## Phase 1: Make the existing foundation trustworthy

| Proposed work | Acceptance gate |
| --- | --- |
| Durable active-session checkpoints and explicit collection gaps | Process kill/restart cannot silently imply uninterrupted history |
| Effective refresh controls | UI displays actual service cadence and separates screen refresh from history recording |
| Correct CSV schema; versioned export contract | Every row parses against its header and round-trip fixtures |
| Deletion boundary for active sessions | Deleting history cannot resurrect pre-delete session data |
| Capability and outcome states | Unsupported, denied, requested, applied and verified are visibly distinct |
| Restore tracking for automation | Original settings survive process death and are restored only when app-owned |
| Resource-based battery UI strings | Battery pages can be localized consistently |

## Phase 2: Strong non-root battery product

Propose session comparison, user-selected history windows, transparent estimation confidence, trend summaries and configurable retention. Explain that foreground-time share is not battery-energy attribution. Improve capacity estimation with calibration/sample sufficiency information, outlier reasoning and known-source labels. Add validated backup import with schema migration and settings scope clearly stated.

Acceptance: usable without privileged prompts; missing sensors remain missing; estimates disclose their inputs; long-running monitor overhead is measured on physical stock devices; denied permissions have clear recovery paths. Exact per-app energy, wakelock ownership and hidden OEM counters must not be promised from public UsageStats alone.

## Phase 3: Verified root charging and diagnostics

Propose per-device capability probing, post-write read-back, explicit hold reasons, root-loss status, recovery workflow and a supported-device record. Separate software charge suspension from verified hardware bypass. Consider richer root diagnostics only where data sources and interpretation can be validated.

Acceptance: hold/resume succeeds on each listed supported device, original values restore after ordinary pause and controlled interruption, external changes are respected, unsupported devices cannot display “protected,” and temperature safeguards remain understandable.

## Phase 4: Carefully scoped automation

Propose charging schedules, profile rules and diagnostic correlations only after persistent ownership/restoration and session recovery are established. Each rule needs conditions, priority, conflict handling, backend requirements, manual override and a user-visible activity record. Avoid inventing capabilities the platform cannot provide without privilege.

Acceptance: overlapping rules resolve deterministically, reboot/backend loss is tested, the user can explain why a rule acted, and disabling a rule restores only app-owned changes.

## Release evidence

For each release record tested device/model, Android version, backend, supported sensors/nodes, monitor duration, idle/active overhead, charge-control outcome and recovery result. Keep unit/simulation evidence separate from physical-device results. A capability is ready to advertise when its complete user flow works under its documented conditions, including denial and recovery.

## Current gaps that motivate this work

The current app has heuristic health confidence, bounded local history, in-memory active sessions, foreground-time app usage, software charging holds, opportunistic reports and broad tuning controls. It does not yet provide universal charging compatibility, exact per-app energy attribution, continuous crash-safe sessions, backup restore, verified hardware passthrough or a durable general automation engine.
