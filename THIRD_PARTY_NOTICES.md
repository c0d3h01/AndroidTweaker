# Third-party notices

The repository's application source retains its existing MIT license in LICENSE. Bundled fonts retain their own license and author notices.

The following information is recorded in the fonts' embedded OpenType name tables (copyright and license records), inspected during the source refactor:

| Bundled files | Embedded copyright | Embedded license |
| --- | --- | --- |
| `app/src/main/res/font/inter.ttf` | Copyright 2016 The Inter Project Authors (https://github.com/rsms/inter) | SIL Open Font License, Version 1.1 |
| `app/src/main/res/font/jetbrains_mono_regular.ttf`, `jetbrains_mono_medium.ttf`, `jetbrains_mono_semibold.ttf` | Copyright 2020 The JetBrains Mono Project Authors (https://github.com/JetBrains/JetBrainsMono) | SIL Open Font License, Version 1.1 |

The embedded license record links to https://openfontlicense.org. This inventory does not replace the complete upstream license text or establish the provenance of every binary revision. Release maintainers should retain the upstream font license files with distributed artifacts and review the resolved dependency notices for that release. No font binaries, application license or dependency licenses were changed by this refactor.

The added `org.json:json:20240303` dependency is used only by local JVM compatibility tests and is not added to the application runtime dependency configuration. Other application dependencies remain declared in the Gradle version catalog and app build script.
