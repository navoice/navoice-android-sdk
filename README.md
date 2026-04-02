# Navoice Android SDK

Current Version: 1.0.0  
Platform: Android  
Distribution: AAR

Official SDK for integrating Navoice voice navigation into Android applications.
Part of the **Navoice Voice Navigation Platform**.

## Overview
Navoice Android SDK enables voice-driven navigation inside Android applications.

Users can speak commands such as:

- "Open events"
- "Show taxes"
- "What is my subscriber number"

The SDK interprets the request and returns a navigation result which your application uses to update UI or navigation state.

The SDK does not control your UI or navigation — your application remains fully in control.

Navoice is UI-agnostic.  
It works with Jetpack Compose, Android Views / XML, and hybrid applications.

## What’s in this repository

This repository includes:

- `navoice-sdk-release.aar` — the Navoice Android SDK
- `Navoice-MyCity` — demo Android application shipped alongside the SDK, demonstrating a complete integration pattern

The demo application is provided as a reference implementation and is disabled until project credentials are configured.

---

## Demo Application: Navoice-MyCity

Navoice-MyCity is a reference Android application shipped alongside the SDK.  
It demonstrates a full end-to-end integration including:

- SDK initialization
- Voice lifecycle
- Result handling
- Navigation mapping
- Presentation handling

The demo application is disabled until you configure your own project credentials.

The demo application is provided for reference only and is not intended for production use.

If the publishable key is missing:

- SDK initialization is disabled
- A warning appears on app launch
- Microphone button shows a warning
- Text input button shows a warning

The application UI continues to run normally.

## Configuring the Demo App

To enable the demo app:

1. Go to Navoice Portal
2. Create a project
3. Copy the publishable key
4. Add the key to `gradle.properties`:

```properties
NAVOICE_PUBLISHABLE_KEY=pk_your_key
NAVOICE_BACKEND_BASE_URL=https://api.navoice.io
```

License validation requires:

- A publishable key
- A package name registered in Navoice Portal

## License Validation

Navoice validates the application using:

- Publishable key
- Android package name (applicationId)

If validation fails:

- Voice features are disabled
- Text routing is disabled
- UI remains functional

Ensure your package name is registered in the Navoice Portal under Allowed Identifiers.

## Key Capabilities

- Voice-driven navigation
- Text routing (cloud interpret pipeline)
- Local + cloud Speech-to-Text (STT)
- Secure license validation
- Spec-based navigation architecture
- Execute, Present, and ShowChoices result types

---

## Requirements

Navoice supports both Jetpack Compose and classic Android Views / XML applications.

- Android minSdk 24
- Kotlin 1.7+
- Java 17


## Installation

1. Copy `navoice-sdk-release.aar` into `app/libs/`.
2. Add the SDK binary to `app/build.gradle.kts`:

```kotlin
implementation(files("libs/navoice-sdk-release.aar"))
```

### Required Dependencies

The AAR does not bundle external runtime dependencies. Add the following to your app module’s `dependencies` in `app/build.gradle.kts`:

```kotlin
implementation("androidx.core:core-ktx:1.12.0")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.moshi:moshi:1.15.0")
implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
implementation("com.squareup.moshi:moshi-adapters:1.15.0")
```

---

## 30-Second Integration

1. Copy the AAR to `app/libs` and add the AAR dependency in `app/build.gradle.kts`.
2. Configure `NavoiceConfig` with your publishable key.
3. Load the spec from assets (for example: app/src/main/assets/spec.json).
4. Set `onResult` to handle navigation.
5. Call `startVoice()` / `stopVoice()` for voice, or `route(text)` for text.

```kotlin
val config = NavoiceConfig.Builder(context)
    .publishableKey("YOUR_PUBLISHABLE_KEY")
    .identifier(context.packageName)
    .build(applicationId = context.packageName)

val navoice = Navoice(config)

navoice.onResult = { result ->
    when (result) {
        is NavoiceResult.Execute -> navigateTo(result.screenId, result.params)
        is NavoiceResult.Present -> showPresentation(result.presentationId, result.params)
        is NavoiceResult.ShowChoices -> showChoiceSheet(result.say, result.choices)
        is NavoiceResult.Unsupported -> showMessage(result.say)
    }
}
```

---

## Basic Setup

```kotlin
val config = NavoiceConfig.Builder(context)
    .baseUrl("https://api.navoice.io")  // optional; defaults to this URL if omitted
    .publishableKey("YOUR_PUBLISHABLE_KEY")
    .identifier(context.packageName)
    .locale("en-US")
    .build(applicationId = context.packageName)

val navoice = Navoice(config).apply {
    specProvider = {
        NavoiceSpecLoader.loadFromAssets(context, "spec")  // loads spec.json from assets
    }
    onResult = { result ->
        when (result) {
            is NavoiceResult.Execute -> navigateTo(result.screenId, result.params)
            is NavoiceResult.Present -> showPresentation(result.presentationId, result.params)
            is NavoiceResult.ShowChoices -> showChoiceSheet(result.say, result.choices)
            is NavoiceResult.Unsupported -> showMessage(result.say)
        }
    }
}
```

---

## App Configuration

### Required

- **publishableKey** – Your Navoice publishable key (from Navoice Portal).
- **identifier** – App identifier (e.g. `context.packageName`). Can be omitted if `build(applicationId)` is used; it will default to `context.packageName`.
- **specProvider** – Suspend function that returns the spec JSON. Must not return an empty map.

### Optional

- **baseUrl** – Backend URL. If not provided, SDK uses internal default: `https://api.navoice.io`.
- **locale** – Default `"en-US"`.
- **sttConfig** – STT mode: `localOnly`, `hybrid`, `cloudOnly`, or `disabled`. Default: `localOnly()`.
- **context** – Android `Context` for STT and asset loading.
- **debug** – Enable debug logging.

---

## Navigation Spec

The app must provide a navigation spec via `specProvider`.

The spec defines:

- screens
- keywords
- examples
- routing logic
- task actions

The spec can be generated using the **Navoice Spec Builder** in the Navoice Portal.

---

## Minimal Spec Example

Place your spec JSON in Android assets. Typical path: `app/src/main/assets/spec.json`.

```json
{
  "app": { "id": "my-app" },
  "routing": {
    "thresholds": {
      "execute_min_score": 6.0,
      "execute_min_conf": 0.75,
      "choices_min_score": 3.0,
      "choices_min_conf": 0.45
    }
  },
  "tasks": [
    {
      "id": "events.open",
      "title": "Events",
      "screenId": "events",
      "keywords": ["events", "scheduled-events"],
      "examples": ["show events", "open events"],
      "defaultParams": {},
      "action": { "type": "navigate" }
    }
  ]
}
```

---

## Initialize SDK

```kotlin
val config = NavoiceConfig.Builder(context)
    .baseUrl("https://api.navoice.io")  // optional
    .publishableKey("pk_xxxxxxxxxxxxxxxxx")
    .identifier(context.packageName)
    .locale("en-US")
    .sttConfig(NavoiceSTTConfig.localOnly())
    .build(applicationId = context.packageName)

val navoice = Navoice(config).apply {
    specProvider = {
        NavoiceSpecLoader.loadFromAssets(context, "spec")
    }
    onResult = { /* handle result */ }
}
```

---

## Android Views / XML Integration

Navoice works with both Jetpack Compose and classic Android Views / XML applications.

If your app is built using Activities, Fragments, Views, or XML layouts, you can initialize and use Navoice directly in your Activity or Fragment.

### Initialize in Activity

```kotlin
class MainActivity : AppCompatActivity() {

    private lateinit var navoice: Navoice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = NavoiceConfig.Builder(this)
            .publishableKey("YOUR_PUBLISHABLE_KEY")
            .identifier(packageName)
            .build(applicationId = packageName)

        navoice = Navoice(config).apply {
            specProvider = {
                NavoiceSpecLoader.loadFromAssets(this@MainActivity, "spec")
            }
            onResult = { result ->
                when (result) {
                    is NavoiceResult.Execute -> navigateTo(result.screenId, result.params)
                    is NavoiceResult.Present -> showPresentation(result.presentationId, result.params)
                    is NavoiceResult.ShowChoices -> showChoiceSheet(result.say, result.choices)
                    is NavoiceResult.Unsupported -> showMessage(result.say)
                }
            }
        }
    }
}
```

### Start Voice

```kotlin
navoice.startVoice()
```

### Example Navigation (Views / XML)

```kotlin
private fun navigateTo(screenId: String, params: Map<String, String>) {
    when (screenId) {
        "events" -> startActivity(Intent(this, EventsActivity::class.java))
        "education" -> startActivity(Intent(this, EducationActivity::class.java))
    }
}
```

### Example Presentation

```kotlin
private fun showPresentation(presentationId: String, params: Map<String, String>) {
    val sheet = PublishableKeyBottomSheet()
    sheet.show(supportFragmentManager, "publishable-key")
}
```

---

## UI Framework Support

Navoice is UI-agnostic and works with:

- Jetpack Compose
- Android Views / XML
- Hybrid applications

---

## Threading

All Navoice callbacks are safe to use for UI updates from the main application flow.

If your app uses lifecycle-aware scopes or custom threading, keep your navigation and UI updates on the main thread.

---

## Minimal Example

```kotlin
navoice.onResult = { result ->
    when (result) {
        is NavoiceResult.Execute -> println("Navigate to: ${result.screenId}")
        is NavoiceResult.Present -> println("Present: ${result.presentationId}")
        is NavoiceResult.Unsupported -> println(result.say)
        is NavoiceResult.ShowChoices -> println(result.say)
    }
}

navoice.startVoice()
```

---

## Handling Unsupported Commands

```kotlin
is NavoiceResult.Unsupported -> {
    showMessage(result.say)
}
```

---

## App Lifecycle

Stop voice when the app goes to background or when the current screen is no longer active:

```kotlin
navoice.stopVoice()
```

Restart when needed.

---

## Full Result Handling Example

The SDK returns `NavoiceResult` via `onResult` or from `route(text)`.

```kotlin
when (result) {
    is NavoiceResult.Execute -> {
        // Navigate to screen
        navigateTo(result.screenId, result.params)
    }
    is NavoiceResult.Present -> {
        // Show presentation (modal, bottom sheet)
        showPresentation(result.presentationId, result.params)
    }
    is NavoiceResult.ShowChoices -> {
        // Show choice sheet
        showChoiceSheet(result.say, result.choices)
    }
    is NavoiceResult.Unsupported -> {
        // No match or fallback
        showMessage(result.say)
    }
}
```

---

## Result Types

### Execute

Navigate to a screen.

| Field       | Type                 | Description        |
|------------|----------------------|--------------------|
| screenId   | String               | Target screen ID   |
| params     | Map<String, String>   | Route parameters   |
| say        | String               | User-facing message|
| confidence | Double?               | Match confidence   |

### Present

Show a presentation (modal, bottom sheet). Use when a task defines `action.type == "present"`.

| Field           | Type                 | Description        |
|-----------------|----------------------|--------------------|
| presentationId  | String               | Presentation ID    |
| params          | Map<String, String>  | Route parameters   |
| say             | String               | User-facing message|

### ShowChoices

Display multiple choices for the user to select. Mainly when cloud returns choices.

| Field   | Type                     | Description        |
|---------|---------------------------|--------------------|
| say     | String                     | User-facing message|
| choices | List&lt;NavoiceChoice&gt; | Selectable options |

**NavoiceChoice:**

| Field       | Type                 |
|------------|----------------------|
| taskId     | String               |
| title      | String               |
| confidence | Double               |
| screenId   | String?              |
| params     | Map&lt;String, String&gt;? |

### Unsupported

No match or fallback.

| Field | Type   | Description        |
|-------|--------|--------------------|
| say   | String | User-facing message|

---

## Voice Lifecycle

1. **Request permission** – Call `requestVoicePermission(activity, callback)` from an Activity.
2. **Forward permission result** – Call `onRequestPermissionsResult(requestCode, grantResults)` when `requestCode == LocalSpeechRecognizer.REQUEST_RECORD_AUDIO`.
3. **Start listening** – `navoice.startVoice()`.
4. **Stop listening** – `navoice.stopVoice()`.
5. **Handle result** – `onResult` is invoked with the routing result.

Voice routing uses a local-first pipeline:
local matcher → semantic resolver → cloud interpret fallback.

---

## Text Routing

```kotlin
// Suspend (requires coroutine scope)
lifecycleScope.launch {
    val result = navoice.route("show my subscriber number")
    when (result) {
        is NavoiceResult.Execute -> navigateTo(result.screenId, result.params)
        // ...
    }
}

// Async callback
navoice.routeAndCallback("show my subscriber number")
// onResult is invoked when done
```

Text routing currently uses the cloud flow. `route(text)` calls `/api/interpret` and maps the response to `NavoiceResult`.

---

## UI Integration

The SDK does not provide UI components. It does not control your app's screens, navigation, or layout. You are responsible for:

- Mic button or floating mic UI
- Navigation (e.g. `NavController`, `FragmentManager`, `Intent`)
- Choice sheet or bottom sheet for `ShowChoices`
- Presentation modals for `Present`
- Error and permission messages

---

## Responsibilities

| App responsibility | SDK responsibility |
|--------------------|--------------------|
| Provide spec via `specProvider` | Load and validate spec |
| Implement navigation | Return `NavoiceResult` |
| Implement UI (mic, sheets, modals) | Return result kinds and payloads |
| Handle permissions | Request RECORD_AUDIO |
| Provide `Context` | Use for STT and asset loading |

---

## Architecture

```
App (Activity/Fragment)
    │
    │ specProvider()
    ▼
Navoice SDK
    │
    │ POST /api/license/validate
    ▼
Navoice Backend (License Validation)
    │
    │ JWT License Token
    ▼
Navoice SDK (Active)
    │
    │ Voice: Local STT → routeText → Local/Semantic/Cloud → mapResponse
    │ Text:  route(text) → POST /api/interpret → mapResponse
    ▼
NavoiceResult → onResult (App)
```

---

## Licensing & Security

- License validation: `POST /api/license/validate` with `publishable_key`, `platform`: `"android"`, `identifier`.
- Token is cached locally and refreshed when expired.
- License must be valid before `/api/interpret` or `/api/stt` calls.

---

## Production Configuration

```kotlin
val config = NavoiceConfig.Builder(context)
    .baseUrl("https://api.navoice.io")  // optional; defaults internally
    .publishableKey(BuildConfig.NAVOICE_PUBLISHABLE_KEY)
    .identifier(context.packageName)
    .locale("en-US")
    .sttConfig(NavoiceSTTConfig.hybrid())  // cloud fallback when local fails
    .debug(BuildConfig.DEBUG)
    .build(applicationId = context.packageName)
```

Store the publishable key in `BuildConfig` or a secure config; do not hardcode in source.

---

## Error Handling

- **Empty spec** – SDK returns `NavoiceResult.Unsupported("Missing spec. Please provide specProvider (e.g., load spec.json from assets).")`.
- **License failure** – SDK returns `NavoiceResult.Unsupported("Your license is not active. Contact your administrator or purchase a subscription.")`.
- **Empty/whitespace text** – SDK returns `NavoiceResult.Unsupported("Please type or say something 🙂")`.
- **Network/parse errors** – `routeAndCallback` invokes `onResult` with `NavoiceResult.Unsupported(errorMessage)`.

---

## Integration Checklist

- [ ] Add `navoice-sdk-release.aar` to `app/libs`
- [ ] Add required SDK runtime dependencies to `app/build.gradle.kts`
- [ ] Add `spec.json` to `app/src/main/assets/`
- [ ] Configure `NavoiceConfig` with publishable key
- [ ] Set `specProvider` to load spec from assets
- [ ] Set `onResult` to handle Execute, Present, ShowChoices, Unsupported
- [ ] Implement navigation (screenId → screen)
- [ ] Implement presentation UI for `Present`
- [ ] Implement choice sheet for `ShowChoices`
- [ ] Add mic button and call `startVoice()` / `stopVoice()`
- [ ] Request `RECORD_AUDIO` and forward to `onRequestPermissionsResult`
- [ ] (Optional) Add text input and call `route(text)` or `routeAndCallback(text)`

---

## Binary Distribution

- Delivered as `navoice-sdk-release.aar`
- Source code is not included
- The host app must add the runtime dependencies listed in the Installation section.

---

## Support
support@navoice.io
For issues, feature requests, or documentation: contact your Navoice support channel or refer to the Navoice Portal.
