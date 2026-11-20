# kmp-weather

A weather app in Kotlin Multiplatform with Compose Multiplatform: search for a
place, see its forecast. One of four builds of the same app, alongside
[flutter-weather](https://github.com/calemccammon/flutter-weather),
[react-native-weather](https://github.com/calemccammon/react-native-weather) and
[maui-weather](https://github.com/calemccammon/maui-weather),
to compare the frameworks.

The repository holds the project set-up only. The code under `shared/`,
`androidApp/`, `desktopApp/`, `webApp/` and `iosApp/` is the sample from the
[Kotlin Multiplatform wizard](https://kmp.jetbrains.com) (Android, iOS,
desktop and web with shared Compose UI, tests included); the app is still to be
written. The work is laid out as [milestones](../../milestones) and
[issues](../../issues).

## Data

- Place search and forecasts: [Open-Meteo](https://open-meteo.com/) (no key;
  free for non-commercial use; CC BY 4.0, attribution required).
- Map tiles (map milestone): [OpenFreeMap](https://openfreemap.org/) styles
  over OpenStreetMap data, drawn with MapLibre. No key.

## Libraries already added

All are declared in `gradle/libs.versions.toml` and wired into
`shared/build.gradle.kts`.

| Purpose | Library |
| --- | --- |
| HTTP | Ktor client (`okhttp` engine on Android and desktop, `darwin` on iOS, `js` on web) |
| JSON models | `kotlinx-serialization-json` |
| Dates | `kotlinx-datetime` |
| Dependency injection | Koin (`koin-core`, `koin-compose-viewmodel`) |
| Navigation | `navigation-compose` (JetBrains multiplatform build) |
| Saved places and settings | `multiplatform-settings-no-arg` |
| Map | `maplibre-compose` (Android, iOS, desktop and the JS web target; not Wasm) |
| Tests | `kotlin-test`, `kotlinx-coroutines-test`, `ktor-client-mock` |
| Static analysis | ktlint (`.editorconfig`), detekt (`config/detekt/detekt.yml`), Android Lint |

Any of these can be swapped; nothing depends on them yet. There is no location
library: see the "Forecast for my current location" issue.

The desktop target is there so the shared UI can be run on Linux without an
emulator. iOS builds need a Mac.

The web app has two targets, as the wizard sets it up: Wasm (the default, and
what CI and Pages build) and JS. MapLibre Compose has no Wasm build, so it is
declared per platform in `shared/build.gradle.kts` instead of in `commonMain`;
see the map issues for what that means for the map screen.

## Static analysis

```sh
./gradlew ktlintCheck detekt :androidApp:lintDebug
./gradlew ktlintFormat     # fixes what ktlint can fix by itself
```

## CI

`.github/workflows/ci.yml` runs on pushes to `main` and on pull requests:
ktlint, detekt and Android Lint, the
shared module's desktop, Android host and web tests, then the Android debug
APK and the Wasm web app (both uploaded as workflow artifacts) and the desktop
app.

## GitHub Pages

`.github/workflows/pages.yml` builds the web app and publishes it to
`https://calemccammon.github.io/kmp-weather/`. It runs only when started by hand
from the Actions tab. On the free plan Pages needs a public repository, so it
will fail until this one is public.

---

The rest of this file is the wizard's own README.

This is a Kotlin Multiplatform project targeting Android, iOS, Web, Desktop (JVM).

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- Desktop app:
  - Hot reload: `./gradlew :desktopApp:hotRun --auto`
  - Standard run: `./gradlew :desktopApp:run`
- Web app:
  - Wasm target (faster, modern browsers): `./gradlew :webApp:wasmJsBrowserDevelopmentRun`
  - JS target (slower, supports older browsers): `./gradlew :webApp:jsBrowserDevelopmentRun`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- Desktop tests: `./gradlew :shared:jvmTest`
- Web tests:
  - Wasm target: `./gradlew :shared:wasmJsTest`
  - JS target: `./gradlew :shared:jsTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://kotlinlang.org/compose-multiplatform/),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).