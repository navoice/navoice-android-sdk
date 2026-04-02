# MyCity Android

Android version of the MyCity iOS app. Matches features, flows, and navigation 1:1.

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- minSdk 24, targetSdk 34

## Project Structure

```
MyCity-Android/
├── app/
│   ├── src/main/
│   │   ├── assets/
│   │   │   └── mycity_spec.json      # Spec (same as iOS)
│   │   ├── java/com/navoice/mycity/
│   │   │   ├── config/               # AppConfig
│   │   │   ├── navigation/           # RouteMapping (screenId → tab)
│   │   │   ├── sdk/                  # NavoiceManager, NavoiceResult
│   │   │   └── ui/
│   │   │       ├── main/             # RootTabsActivity, ViewModel
│   │   │       └── tabs/             # Education, Events, Recycle, Taxes fragments
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Setup

1. **Open in Android Studio**
   ```
   File → Open → select MyCity-Android folder
   ```

2. **Sync Gradle**
   - Android Studio will prompt for sync, or use `File → Sync Project with Gradle Files`
   - If the Gradle wrapper is missing, run in terminal: `gradle wrapper` (requires Gradle installed)

3. **Configure Navoice (optional)**
   - Add to `gradle.properties` or `local.properties`:
     ```properties
     NAVOICE_PUBLISHABLE_KEY=pk_your_key_here
     NAVOICE_BACKEND_BASE_URL=https://api.navoice.io
     ```
   - If not set, backend URL defaults to the URL above; publishable key must be provided for SDK init.

4. **Navoice Android SDK**
   - When the Navoice Android SDK is available, add the dependency in `app/build.gradle.kts`:
     ```kotlin
     implementation("io.navoice:sdk-android:x.x.x")  // or your maven coordinates
     ```
   - Wire it in `NavoiceManager.kt` (see TODOs in that file).
   - Init params: publishableKey, backendBaseUrl, identifier=packageName (dynamic), sttConfig="hybrid".

## Run

1. Connect a device or start an emulator
2. `Run → Run 'app'` or click the green Run button
3. Grant microphone permission when prompted

## Features

- **Tabs**: Education, Events, Recycle, Taxes (matches iOS)
- **Route mapping**: spec.json `screenId` → tab (education, events, recycle, taxes)
- **Floating mic button**: bottom-right, states: idle (mic icon) / listening (stop icon) / thinking (progress)
- **Result handling**:
  - `execute(screenId)` → navigate to the mapped tab
  - `choices` → show dialog list to pick
  - `unsupported` → Snackbar + red badge

## Permissions

- `RECORD_AUDIO` – for voice input
- `INTERNET` – for SDK backend

## Architecture

- **AppConfig**: Central config from BuildConfig (no hardcoded keys/URLs)
- **NavoiceManager**: Single entry point for SDK (startVoice, stopVoice, handleResult)
- **RouteMapping**: screenId → tab index (aligned with spec.json)
- **RootTabsActivity**: Hosts NavHostFragment + BottomNav + floating mic
- **Fragments**: Education, Events, Recycle, Taxes (skeleton content matching iOS)
