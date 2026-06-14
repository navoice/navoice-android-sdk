# MyCity-aOS — CLAUDE.md

## Project purpose

A reference / demo Android app demonstrating Navoice SDK integration in a municipal services context. Same feature set as `MyCity-iOS` and `MyCity-Web` — Taxes, Recycle, Events, Education — navigable by voice. Used for demos and as an integration example for Android SDK customers.

## Tech stack

- **Language:** Kotlin
- **UI framework:** Android Views (XML layouts, Fragments) with MVVM (ViewModel)
- **Build system:** Gradle (Kotlin DSL, Gradle 8.x)
- **SDK integration:** `navoice-sdk.aar` (pre-built, in `app/libs/`)
- **Navigation:** Android Navigation Component (`nav_graph.xml`, Bottom Nav)
- **No third-party UI libraries** beyond Android Jetpack

## Important folders

```
app/src/main/
  java/com/navoice/mycity/
    config/
      AppConfig.kt            Navoice publishable key, backend URL
    navigation/
      RouteMapping.kt         Maps Navoice screenId → Android navigation destination
    sdk/
      NavoiceManager.kt       SDK lifecycle manager (init, start/stop listening)
      NavoiceResult.kt        Result type wrapper
    ui/main/
      RootTabsActivity.kt     Main activity with bottom navigation
      RootTabsViewModel.kt    ViewModel for tab state and Navoice results
    ui/tabs/
      EducationFragment.kt    Education tab
      EventsFragment.kt       Events tab
      RecycleFragment.kt      Recycle tab
      TaxesFragment.kt        Taxes tab
  res/
    layout/                   XML layouts for activity and fragments
    navigation/nav_graph.xml  Navigation graph
    menu/bottom_nav_menu.xml  Bottom navigation items
  assets/
    mycity_spec.json          Navoice spec (task → navigation destination)
  AndroidManifest.xml
app/libs/
  navoice-sdk.aar             Pre-built Navoice Android SDK
```

## Important files

| File | Purpose |
|---|---|
| `app/src/main/java/com/navoice/mycity/config/AppConfig.kt` | Publishable key + backend URL constants |
| `app/src/main/java/com/navoice/mycity/sdk/NavoiceManager.kt` | SDK init, recording lifecycle, result handling |
| `app/src/main/java/com/navoice/mycity/navigation/RouteMapping.kt` | Maps Navoice screenId strings to nav graph destination IDs |
| `app/src/main/assets/mycity_spec.json` | Navoice spec — matches voice commands to screenIds |
| `app/libs/navoice-sdk.aar` | Pre-built SDK — do not modify; rebuild from SDK-aOS |
| `app/src/main/AndroidManifest.xml` | App manifest (permissions, activities) |
| `app/src/main/res/navigation/nav_graph.xml` | Navigation graph |
| `app/build.gradle.kts` | App module build config |
| `settings.gradle.kts` | Multi-module Gradle settings |

## Environment variables

None — Android app. All configuration is in `AppConfig.kt`:

```kotlin
object AppConfig {
    const val PUBLISHABLE_KEY = "pk_..."
    const val BACKEND_BASE_URL = "https://api.navoice.io"  // or similar
}
```

(Actual values — needs confirmation by reading `AppConfig.kt`.)

## External services

- **Navoice Backend** (via SDK AAR) — license validate, STT, interpret
- **Android SpeechRecognizer** (device) — local STT

## How this project connects to the rest of Navoice

- Integrates `navoice-sdk.aar` (built from SDK-aOS).
- Uses `mycity_spec.json` which mirrors the spec in `MyCity-iOS` and `MyCity-Web`.
- Demonstrates the same user flows as the iOS and Web demo apps, on Android.

## Do-not-break rules

- **`app/libs/navoice-sdk.aar`** — do not modify binary. Rebuild from SDK-aOS and re-copy if SDK changes.
- **`mycity_spec.json` task IDs** — must match destination IDs in `nav_graph.xml` via `RouteMapping.kt`. Mismatches cause silent navigation failures.
- **`RECORD_AUDIO` permission** — must be declared in `AndroidManifest.xml`. Required for SDK mic capture.
- **`NavoiceManager.kt` lifecycle** — SDK recording must be stopped in `onPause()`/`onStop()`. Leaking an active `AudioRecord` causes system-level audio issues.

## Common development tasks

- **Update the SDK:** `./gradlew assembleRelease` in SDK-aOS → copy `navoice-sdk.aar` to `app/libs/`.
- **Update the spec:** edit `app/src/main/assets/mycity_spec.json`. Update `RouteMapping.kt` if IDs change.
- **Add a new tab:** add a Fragment + layout, add a nav destination in `nav_graph.xml`, add a `RouteMapping` entry, add a task in `mycity_spec.json`.

## Build / run / test commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Lint
./gradlew lint
```

Open in Android Studio for device/emulator management.

## Known risks

- `navoice-sdk.aar` is a manually managed binary — easy to become out of sync with SDK-aOS.
- `mycity_spec.json` must be synced manually across all three MyCity apps.
- `local.properties` contains `sdk.dir` — must not be committed to git.
- The `.gitmodules` file exists — suggests a git submodule is or was configured. Needs confirmation on whether it is active.
- `RECORD_AUDIO` and possibly `INTERNET` permissions must be in the manifest for SDK to function.
