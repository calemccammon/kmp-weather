# kmp-weather

A weather app in Kotlin Multiplatform, with its screen shared through Compose
Multiplatform. One screen: a map with a Material 3 search bar over it. Search
for a place, or press and hold on the map, and the forecast for the week
appears.

**Live demo:** <https://calemccammon.github.io/kmp-weather/>

| Wide window                                                                                                 | Phone                                                                                         |
| ----------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------- |
| ![The app on a wide window: a side panel with the week's forecast beside the map](docs/screenshot-wide.png) | ![The app on a phone: the forecast in a bottom sheet over the map](docs/screenshot-phone.png) |

It is one of four builds of the same app, alongside
[flutter-weather](https://github.com/calemccammon/flutter-weather),
[react-native-weather](https://github.com/calemccammon/react-native-weather) and
[maui-weather](https://github.com/calemccammon/maui-weather),
to compare the frameworks.

## What it does

- **Search** with results as you type, after a short pause; an answer to an
  older search never replaces a newer one.
- **Pick a point** by pressing and holding on the map, or with the mouse's
  second button.
- **Forecast**: current conditions, then seven days with the chance of
  precipitation and each day's range drawn against the week's.
- **Adapts to the window**: a draggable bottom sheet on a phone, a side panel
  from 840 dp wide. The map keeps the chosen place clear of both.
- **Celsius or Fahrenheit**, chosen on the forecast and remembered between
  launches. Until you choose, it follows the device's region.
- **Light and dark** themes from one seed colour, following the system. The
  map changes style with them.
- **Accessible**: each forecast block is read as one sentence by a screen
  reader, what is loading or has failed is announced, and the forecast's
  columns grow with the device's text size.
- **Says what went wrong**: a timeout, no connection, a refused request and a
  service in trouble each have their own message and a way to try again.

## Architecture

Clean Architecture in three layers. Dependencies point inward only: `data`
and `ui` import `domain`, and `domain` imports neither, nor any library but
Kotlin's own (`kotlinx-datetime` for its dates). `ArchitectureTest` reads
every source file's imports and fails on one that breaks this.

All of it is in the `shared` module, under
`shared/src/commonMain/kotlin/com/calemccammon/weather/kmp`:

| Path     | What it holds                                                                                                                                                                                                                                                                                                 |
| -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `domain` | The app's own types (`Place`, `Forecast`, `WeatherCondition`, `Units`), the repository interfaces (`WeatherRepository` with its failure kinds, `SettingsRepository`), and the use cases (`SearchPlaces`, `GetForecast`, `GetUnits`, `SetUnits`) that hold the rules. Plain Kotlin: no Compose, no Ktor.          |
| `data`   | Open-Meteo: the wire-shaped models (`*Dto`), their translation into domain types, and the repository implementation over Ktor. Settings are kept in each platform's own key-value store.                                                                                                                       |
| `ui`     | `core` has the theme, its design tokens and the mapping from domain values to wording and icons. `home` has `models` (what the screen shows, already rounded and worded), `viewmodels` (call use cases and produce those models) and `components` (draw them, and import nothing from the domain or data).      |
| `di`     | The Koin modules that say which implementation stands behind each interface and how each use case and view model is built. Nothing else imports from here but each platform's entry point, which calls `startDependencies()`.                                                                                  |

Every string the user sees or hears is in
`shared/src/commonMain/composeResources/values/strings.xml`. A view model
cannot look a string up, so it names the string and what fills its gaps
(`UiText`), and the words are found when the screen is drawn.

Each layer has its own model of the same thing, and a translation between
them:

|              | Data                           | Domain       | UI                                                                         |
| ------------ | ------------------------------ | ------------ | -------------------------------------------------------------------------- |
| Type         | `ForecastDto`                  | `Forecast`   | `ForecastUiModel`                                                          |
| A day's high | `temperature_2m_max[1] = 64.3` | `high: 64.3` | `high`: "64°", `barEnd: 0.75`                                              |
| Conditions   | `weather_code: 3`              | `Overcast`   | an icon and "Overcast"                                                     |
| A failure    | HTTP 400, a timeout            | `Rejected`   | `failure_rejected`: "The weather service refused the request."             |

State is held by two view models, `HomeViewModel` and `SearchViewModel`, each
an `androidx.lifecycle.ViewModel` exposing one `StateFlow`. A request that has
been overtaken (another place selected, more text typed) is cancelled, so its
answer cannot arrive late.

The platform modules are thin. `androidApp`, `desktopApp`, `webApp` and
`iosApp` each start the dependencies and show the shared `App()`. Inside
`shared`, the only code that differs by platform is how the device's country
is read, and the view controller that iOS is handed.

## Running

Kotlin 2.4, Compose Multiplatform 1.12. Targets: Android, desktop (JVM), web
and iOS.

```sh
./gradlew :androidApp:installDebug          # onto a running emulator or device
./gradlew :desktopApp:run                   # a desktop window
./gradlew :webApp:jsBrowserDevelopmentRun   # in a browser, rebuilding on change
./gradlew :webApp:jsBrowserDistribution     # static site in webApp/build/dist/js/productionExecutable
```

Gradle needs Java 21 to run. The desktop app is built and run with Java 25,
which MapLibre's desktop build needs; Gradle downloads it the first time.

The iOS app is in `iosApp`, to open in Xcode on a Mac. Its Kotlin code is
compiled in CI, but the app itself has not been built or run.

## Checks

```sh
./gradlew ktlintCheck detekt :androidApp:lintDebug   # `./gradlew ktlintFormat` fixes formatting
./gradlew :shared:jvmTest                            # every test
./gradlew :shared:testAndroidHostTest :shared:jsTest # the domain and data tests again, on Android and in a browser
```

The tests of the UI layer draw the screen in a desktop window that is never
shown, with a stand-in for the map, so they run on the JVM only. The browser
tests need Chrome; set `CHROME_BIN` if it is not where Karma looks.

`.github/workflows/ci.yml` runs all of these on pushes to `main` and on pull
requests, compiles the iOS code, then builds the Android debug APK, the
desktop app and the web app. The APK and the web app are uploaded as workflow
artifacts.

`.github/workflows/pages.yml` builds the web app and publishes it to
`https://calemccammon.github.io/kmp-weather/`. It runs only when started by
hand from the Actions tab, and needs the repository to be public.

## Data and credits

- Place search and forecasts: [Open-Meteo](https://open-meteo.com/) (no key;
  free for non-commercial use; CC BY 4.0).
- Map: [OpenFreeMap](https://openfreemap.org/) styles and tiles (no key),
  from [OpenMapTiles](https://openmaptiles.org/) and
  © [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors,
  drawn with [MapLibre](https://maplibre.org/) through
  [MapLibre Compose](https://maplibre.org/maplibre-compose/).

## Known limits

- **English only.** The wording is all in one `strings.xml`, weekday names
  included, so a second language is one more file; none has been written.
- **Places are not saved**: the chosen place is forgotten when the app
  closes. Only the choice of units is kept.
- **The web app is JavaScript, not WebAssembly.** MapLibre Compose has no
  Wasm build, and its API is not yet stable.
- **The desktop app is set up for Linux on x64.** MapLibre ships its native
  code per system; another one needs its own artifact in
  `desktopApp/build.gradle.kts`.
- **With a mouse, a point is picked with the second button.** Holding the
  first one down does nothing.
- **Very large text** widens the forecast's columns and drops the range bar
  when there is no room, but the rows do not shrink to fit beyond that.

## Not built

Ideas that were considered and left out, roughly in the order they would be
worth doing:

- **Saved places** shown when the app opens.
- **Forecast for the current location**, which needs the location permission
  on each platform.
- **A second language**, to exercise the localisation set-up.
