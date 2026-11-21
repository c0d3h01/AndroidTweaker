# AndroidTweaker specifications

Source baseline: working tree reviewed on 2026-11-20, app version 2.0.0 (201), min API 26, target/compile API 36. These documents describe source behavior, not a claim of physical-device certification. Existing uncommitted application changes are included in this baseline.

## Product goal

Build an advanced battery application that is useful without root and gains verified, device-compatible controls with root. Battery measurement, understandable evidence, low monitoring overhead, and reliable recovery take priority over the number of tuning switches. “Most advanced working app” is the product ambition, not a claim that every planned capability exists or every device supports every control.

## Reading guide

| Group | Specification | Contents |
| --- | --- | --- |
| Product | [App structure](01-product-and-navigation.md) | Current pages, navigation and ownership |
| Access | [Capabilities](02-access-and-permissions.md) | Non-root, Usage Access, Shizuku and root |
| Battery | [Live monitoring](03-battery-monitoring.md) | Measurements, service, scheduling and lifecycle |
| Battery | [Health and charging analysis](04-health-and-charging-analysis.md) | Capacity, health, quality and session math |
| Battery | [Usage and history](05-usage-history-and-data.md) | Drain, sleep, app usage, reports and exports |
| Battery | [Alerts and integrations](06-alerts-notifications-and-widgets.md) | Alarms, notification, widget and tile |
| Root controls | [Charging protection](07-root-charging-controls.md) | Limits, holds, temperature and restoration |
| Tuning | [Profiles and features](08-tuning-profiles-and-features.md) | Every feature switch and engine group |
| Device | [Device and diagnostics](09-device-settings-and-diagnostics.md) | Hardware, access settings, tools and logs |
| Engineering | [Architecture and validation](10-architecture-and-validation.md) | Data flow, persistence, failure cases and checks |
| Direction | [Battery-first roadmap](11-battery-product-roadmap.md) | Explicitly proposed work and acceptance gates |

## Status language

**Implemented** means a source path exists. **Conditional** means permissions, driver support, OEM policy or sufficient history determine availability. **Gap** identifies a current limitation. **Proposed** means future work and must not be advertised as shipped.

Each feature document includes source references and verification expectations. Paths are relative to this directory. Kotlin source links point to the actual implementation; shell behavior belongs to the bundled assets. Update the corresponding spec when behavior changes.

## Source architecture plan

See the [source architecture](../architecture/README.md), [source map](../architecture/source-map.md) and [implementation ledger](../architecture/progress.md) for feature ownership and completed migration details.
