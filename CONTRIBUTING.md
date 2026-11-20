# Contributing

Start with the [source architecture guide](docs/architecture/README.md) and [feature specifications](docs/specs/README.md). The app has one Gradle module and feature-oriented Kotlin packages. Keep changes small enough to review by behavior.

## Build and checks

Use JDK 17 and an Android SDK with API 36. Set `JAVA_HOME` and `ANDROID_HOME`, then run:

```sh
make doctor
make check
./gradlew :app:assembleDebug :app:assembleRelease :app:lintDebug
```

`make release` stages a signed development build; see [signing instructions](app/README.md). The committed development key is public. Do not use it as a private distribution identity or commit private signing credentials. Existing installations require a compatible certificate for upgrades.

## Make a change

Find the owning feature package first. Battery calculations belong in `battery/analysis`; monitoring decisions in `battery/monitor`; charging holds in `charging`; transport in `access`; dispatcher execution in `engine`. Keep feature panels local and share only presentation used by several features.

Use two-space Kotlin/shell indentation, PascalCase types/composables, camelCase functions/properties and snake_case resource names. Put new user-facing strings in Android resources. Prefer names that describe the job over Helper/Manager/Utils abstractions. Add comments for units, device differences and recovery invariants.

For behavior changes, add regression tests that exercise the real calculation/state transition. Move tests with their production feature. Run JVM tests, shell checks, build and lint. Shell assets require `/system/bin/sh` compatibility, quoted paths and an engine version bump when changed.

Preserve preference keys, profile codes, external intent values, notification channels and Android component identities. A source rename does not authorize changing an installed-state contract. See the [compatibility inventory](docs/architecture/compatibility.md).

## Device validation

Non-root paths must work without root prompts. Test denied notifications/Usage Access and absent/denied Shizuku/root. Charging or boot changes also need a compatible rooted physical device: record model, Android version, backend, actual hold/release/read-back and recovery/reboot results. Unit tests or a successful shell exit do not establish hardware compatibility.

## Pull requests and reports

Use Conventional Commit messages. Explain the user-visible result, linked issue, verification and limitations; attach screenshots for UI changes. Update the corresponding feature spec. Separate structural moves from changes in defaults or algorithms.

Use repository issues for reproducible bugs and feature proposals. For sensitive reports, use the repository's private security-reporting option if the maintainer has enabled it; do not post private credentials or unpublished exploit details in a public issue. No private reporting address is established by this document.

Retain the existing LICENSE and review [third-party notices](THIRD_PARTY_NOTICES.md). Check provenance and redistribution notices for any added code, fonts or assets. Public-release maintainers must complete the bundled-asset notice audit before claiming distribution readiness.
