# Navoice Android SDK

**Version:** 1.0.0  
**Distribution:** `navoice-sdk-release.aar`

Voice navigation SDK for Android. Users speak (or you send text); the SDK returns structured results so your app performs navigation and UI updates.

This package includes the SDK AAR and the **Navoice-MyCity** demo app (reference only; disabled until you add credentials).

---

## 1. Overview

- **Execute** — navigate to a screen with optional parameters.
- **Present** / **ShowChoices** — returned when your spec defines those flows; handle them in `onResult` if you use them.
- **Unsupported** — no match, license issue, missing spec, or error message for the user.

The SDK does not draw your UI or run your router. You supply a navigation spec, wire a microphone (or text), and implement `onResult`.

---

## 2. Requirements

| Requirement | Notes |
|-------------|--------|
| **Android minSdk** | 24+ |
| **compileSdk** | 36 |
| **Kotlin** | 1.7+ |
| **Java** | 17 |
| **Android Gradle Plugin** | 8.9+ |

Jetpack Compose, Android Views/XML, and hybrid apps are supported.

---

## 3. Installation (step-by-step)

### 3.1 Add SDK binary

1. Copy **`navoice-sdk-release.aar`** into **`app/libs/`**.
2. If `libs` does not exist: in Android Studio, right-click **`app`** → **New** → **Directory** → name it **`libs`**.

### 3.2 Add dependency

Open **`app/build.gradle.kts`** and add:

```kotlin
dependencies {
    implementation(files("libs/navoice-sdk-release.aar"))
}
```

Click **Sync Now**.

### 3.3 Required runtime dependencies

The AAR does not bundle these; add them to **`app/build.gradle.kts`**. The version numbers below are **examples only** — align them with your project’s Kotlin, AGP, and other AndroidX libraries.

```kotlin
implementation("androidx.core:core-ktx:1.12.0")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.moshi:moshi:1.15.0")
implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
implementation("com.squareup.moshi:moshi-adapters:1.15.0")
```

### 3.4 Manifest permissions

In **`app/src/main/AndroidManifest.xml`** (inside `<manifest>`):

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

---

## 4. Quick start (working example)

Minimal flow: AAR + dependencies → config object → `Navoice` → `specProvider` → `onResult` → `startVoice()` after permission.

See sections 5–10 for focused snippets, and **Full MainActivity Example** for a single end-to-end Compose `MainActivity.kt`.

---

## 5. Initialize SDK

### 5.1 App keys — `AppNavoiceConfig.kt`

Create:

**`app/src/main/java/your/package/name/AppNavoiceConfig.kt`**

Do **not** name this file `NavoiceConfig.kt` (that name is reserved for SDK types).

```kotlin
package your.package.name

object AppNavoiceConfig {
    const val PUBLISHABLE_KEY = "your_publishable_key"
    const val BACKEND_BASE_URL = "https://api.navoice.io"
}
```

Use `BACKEND_BASE_URL` only if you need a non-default backend; the SDK default is `https://api.navoice.io` (see `NavoiceConfig.DEFAULT_BACKEND_URL`).

### 5.2 Create `Navoice` in `onCreate`

```kotlin
import io.navoice.sdk.Navoice
import io.navoice.sdk.NavoiceConfig
import io.navoice.sdk.NavoiceSTTConfig

private lateinit var navoice: Navoice

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val config = NavoiceConfig.Builder(this)
        .publishableKey(AppNavoiceConfig.PUBLISHABLE_KEY)
        .identifier(packageName)
        .locale("en-US")
        .sttConfig(NavoiceSTTConfig.hybrid())
        .build(applicationId = packageName)

    navoice = Navoice(config)
}
```

Optional: custom backend and debug logging.

```kotlin
val config = NavoiceConfig.Builder(this)
    .baseUrl(AppNavoiceConfig.BACKEND_BASE_URL)
    .publishableKey(AppNavoiceConfig.PUBLISHABLE_KEY)
    .identifier(packageName)
    .locale("en-US")
    .sttConfig(NavoiceSTTConfig.hybrid())
    .debug(BuildConfig.DEBUG)
    .build(applicationId = packageName)
```

`NavoiceConfig.Builder(this)` passes `Context` so the SDK can run on-device speech recognition.

### Locale configuration & multilingual semantic search

Voice-based semantic catalog search depends heavily on the locale configured in `NavoiceConfig`.

The locale affects:
- Local Speech-to-Text (STT)
- Cloud STT transcription quality
- Semantic catalog matching accuracy

If the configured locale does not match the spoken language or the catalog language, STT may produce inaccurate transcripts, resulting in:
- Low semantic matching scores
- Incorrect catalog matches
- `Unsupported` routing results

Example:

```kotlin
.locale("he-IL")
```

Use a locale that matches the primary language of your catalog and users.

Examples:

| Catalog Language | Recommended Locale |
|---|---|
| English | `en-US` |
| Hebrew | `he-IL` |
| Spanish | `es-ES` |
| French | `fr-FR` |

Example:

```kotlin
val config = NavoiceConfig.Builder(this)
    .publishableKey(AppNavoiceConfig.PUBLISHABLE_KEY)
    .identifier(packageName)
    .locale("he-IL")
    .sttConfig(NavoiceSTTConfig.hybrid())
    .build(applicationId = packageName)
```

Important:
- Emulator/simulator speech recognition quality may differ significantly from physical devices.
- Always validate multilingual voice flows on real Android devices.

---

## 6. Load spec (assets)

### 6.1 Add the JSON file

Place your spec (exported from the Navoice portal) in assets using the standard filename:

**`app/src/main/assets/spec.json`**

### 6.2 Set `specProvider`

After `navoice = Navoice(config)`:

```kotlin
import org.json.JSONObject

navoice.specProvider = suspend {
    val json = assets.open("spec.json")
        .bufferedReader()
        .use { it.readText() }

    JSONObject(json).toMap()
}
```

`specProvider` must return a **non-empty** `Map<String, Any?>`. An empty map produces `NavoiceResult.Unsupported` with a “missing spec” style message.

### 6.3 JSON helper (`JSONObject` → `Map`)

`JSONObject` has no built-in `toMap()`. Import **`org.json.JSONArray`** and **`org.json.JSONObject`**, then add **`private fun JSONObject.toMap()`** and **`private fun JSONArray.toList()`** (same implementation as in **Full MainActivity Example**): place them **below the `MainActivity` class** in the same file, or move them to a small **`JsonSpec.kt`** as **`internal`** functions if you prefer a slimmer activity file.

---

## 7. Voice lifecycle

Use an **`Activity`** (`ComponentActivity` or `AppCompatActivity`). Typical pattern:

1. Call **`navoice.requestVoicePermission(activity) { granted -> … }`** before **`startVoice()`** when the user turns the mic on.
2. Override **`onRequestPermissionsResult`**: when **`requestCode == LocalSpeechRecognizer.REQUEST_RECORD_AUDIO`**, call **`navoice.onRequestPermissionsResult(requestCode, grantResults)`**.
3. Call **`navoice.stopVoice()`** in **`onPause()`** (or when leaving the screen) so listening does not continue in the background.

A working implementation of all three appears in **Full MainActivity Example**.

---

## 8. Add microphone UI (Jetpack Compose)

### 8.1 Enable Compose in the app module

In **`app/build.gradle.kts`**:

```kotlin
android {
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00") // example BOM; pick a current BOM for your stack
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2") // example; use a version compatible with your Activity KTX
}
```

Wire the Compose compiler to your Kotlin version using the [Compose Compiler Gradle plugin](https://developer.android.com/develop/ui/compose/compiler) (recommended for Kotlin 2.0+) or your project’s existing Compose/Kotlin compatibility setup — see the official [Compose–Kotlin compatibility map](https://developer.android.com/jetpack/androidx/releases/compose-kotlin).

### 8.2 Imports (reference)

Use the imports in **Full MainActivity Example** (next section) as the authoritative list for a Compose host screen.

### 8.3 Compose layout pattern

Use a **`Scaffold`** with **`bottomBar`** for tabs and place the **microphone toggle** in the main content. A complete, copy-paste **`MainActivity.kt`** (PlayIt-style tabs: Guitar / Drums / Recorder / Violin, `selectedTab`, `isListening`, permissions, `spec.json`, and JSON helpers) is in **Full MainActivity Example** below.

---

## Full MainActivity Example

Place this file at **`app/src/main/java/your/package/name/MainActivity.kt`** and set your application’s **`package`** and **`AppNavoiceConfig`** to match.

This example uses **`spec.json`** in assets, **`NavoiceSTTConfig.hybrid()`**, bottom navigation for four instruments, and maps **`screenId`** → tab: **`guitar`** → 0, **`drums`** → 1, **`recorder`** → 2, **`violin`** → 3. **`Present`** / **`ShowChoices`** are not implemented here; add UI when your spec uses those result types.

```kotlin
package your.package.name

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.navoice.sdk.Navoice
import io.navoice.sdk.NavoiceConfig
import io.navoice.sdk.NavoiceSTTConfig
import io.navoice.sdk.model.NavoiceResult
import io.navoice.sdk.stt.LocalSpeechRecognizer
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private lateinit var navoice: Navoice

    private val selectedTab = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = NavoiceConfig.Builder(this)
    .publishableKey(AppNavoiceConfig.PUBLISHABLE_KEY)
    .identifier(packageName)

    // IMPORTANT:
    // Use a locale matching your catalog language.
    // Example: Hebrew catalog -> "he-IL"
    .locale("he-IL")

    .sttConfig(NavoiceSTTConfig.hybrid())
    .build(applicationId = packageName)

        navoice = Navoice(config)

        navoice.specProvider = suspend {
            val json = assets.open("spec.json").bufferedReader().use { it.readText() }
            JSONObject(json).toMap()
        }

        navoice.onResult = { result ->
            when (result) {
                is NavoiceResult.Execute -> navigateTo(result.screenId, result.params)
                is NavoiceResult.Unsupported -> Log.d("NAVOICE", "Unsupported: ${result.say}")
                is NavoiceResult.Present ->
                    Log.d("NAVOICE", "Present presentationId=${result.presentationId} (add UI if your spec uses present)")
                is NavoiceResult.ShowChoices ->
                    Log.d("NAVOICE", "ShowChoices say=${result.say} count=${result.choices.size} (add UI if your spec uses choices)")
            }
        }

        val activity = this
        setContent {
            var isListening by remember { mutableStateOf(false) }
            val tabIndex = selectedTab.intValue
            val tabs = listOf("Guitar", "Drums", "Recorder", "Violin")

            Scaffold(
                bottomBar = {
                    NavigationBar {
                        tabs.forEachIndexed { index, label ->
                            NavigationBarItem(
                                selected = tabIndex == index,
                                onClick = { selectedTab.intValue = index },
                                icon = { Text(label.take(1)) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                ) {
                    Text(text = tabs[tabIndex])
                    Button(
                        onClick = {
                            if (isListening) {
                                navoice.stopVoice()
                                isListening = false
                                Log.d("NAVOICE", "Stopped listening")
                            } else {
                                navoice.requestVoicePermission(activity) { granted ->
                                    if (granted) {
                                        navoice.startVoice()
                                        isListening = true
                                        Log.d("NAVOICE", "Started listening")
                                    } else {
                                        Log.d("NAVOICE", "RECORD_AUDIO denied")
                                    }
                                }
                            }
                        }
                    ) {
                        Text(if (isListening) "Stop mic" else "Voice")
                    }
                }
            }
        }
    }

    private fun navigateTo(screenId: String, params: Map<String, String>) {
        selectedTab.intValue = when (screenId) {
            "guitar" -> 0
            "drums" -> 1
            "recorder" -> 2
            "violin" -> 3
            else -> selectedTab.intValue
        }
        Log.d("NAVOICE", "navigateTo screenId=$screenId params=$params selectedTab=${selectedTab.intValue}")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LocalSpeechRecognizer.REQUEST_RECORD_AUDIO) {
            navoice.onRequestPermissionsResult(requestCode, grantResults)
        }
    }

    override fun onPause() {
        super.onPause()
        navoice.stopVoice()
    }
}

private fun JSONObject.toMap(): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    keys().forEach { key ->
        map[key] = when (val v = get(key)) {
            is JSONObject -> v.toMap()
            is JSONArray -> v.toList()
            else -> if (v === JSONObject.NULL) null else v
        }
    }
    return map
}

private fun JSONArray.toList(): List<Any?> {
    return (0 until length()).map { i ->
        when (val v = get(i)) {
            is JSONObject -> v.toMap()
            is JSONArray -> v.toList()
            else -> if (v === JSONObject.NULL) null else v
        }
    }
}
```

---

## 9. Handle results

Assign **`navoice.onResult`** and branch on **`NavoiceResult`** (see **Full MainActivity Example** for **`Execute`**, **`Unsupported`**, and **`Log.d`** stubs for **`Present`** / **`ShowChoices`**).

**Text routing (optional):** from a coroutine scope (e.g. `lifecycleScope`):

```kotlin
lifecycleScope.launch {
    val result = navoice.route("show my subscriber number")
    // same when (result) { … } as above
}
```

Or use `navoice.routeAndCallback("…")` to deliver the same `NavoiceResult` through `onResult`.

---

## 10. Navigation example

Map each **`screenId`** from your Navoice spec to your app: **`NavController`**, **`Intent`**, **`FragmentManager`**, or a tab index. The **Full MainActivity Example** maps **`guitar` / `drums` / `recorder` / `violin`** to **`selectedTab`** (0–3). The **Android Views / XML integration** snippet uses the same IDs with **`Log.d`** placeholders you can replace with real navigation calls.

Register your app’s package name as an allowed identifier in the Navoice portal; otherwise license validation fails and voice/text routing are disabled.

---

## 11. Semantic Catalog Display Fields (`params.display`)

### 11.1 Semantic Catalog Result Structure

Catalog search results may include a `params.display` object alongside `params.itemId`. This object carries human-readable fields extracted from the matched catalog item, so your app can present meaningful information without additional data fetches.

Example `NavoiceResult.Execute` payload for a catalog match:

```json
{
  "kind": "execute",
  "screenId": "catalogItemDetails",
  "params": {
    "itemId": "TITL0000000000000027",
    "display": {
      "title": "The Shawshank Redemption",
      "actors": [
        "Tim Robbins",
        "Morgan Freeman"
      ],
      "image": "https://...",
      "description": "Two imprisoned men bond over a number of years..."
    }
  }
}
```

In Kotlin, `params` is `Map<String, Any?>`, so `params["display"]` will be a `Map<*, *>` when display fields are present.

### 11.2 Generic Display Fields

Display fields are defined in your Semantic Catalog mapping on the Navoice portal using `displayFields`. Each key in `displayFields` names a field to expose in `params.display`, and its value is a dot-path (with optional array notation) into the catalog item's raw JSON.

Example `displayFields` configuration:

```json
"displayFields": {
  "title": "title",
  "actors": "credits.actors",
  "image": "media[].url",
  "description": "synopsis"
}
```

At query time, the SDK server extracts these values from `semantic_items.raw` and populates `params.display` dynamically. No database migration, catalog re-save, or catalog re-sync is required — existing raw data is used as-is.

This mechanism is fully generic and works for any catalog domain:

- Movies and TV series
- Products and e-commerce catalogs
- Restaurants
- Documents
- Real estate listings
- Medical providers (e.g. dentists, doctors)
- Any custom JSON catalog

### 11.3 Client Rendering Recommendations

Different catalog domains use different field names for the primary human-readable label. Use the following fallback order when choosing a label to display:

1. `params.display.title`
2. `params.display.name`
3. `params.display.label`
4. `params.itemId`

Domain examples:
- Movies → `title`
- Products → `name`
- Generic items → `label`
- Final fallback → `itemId`

**JavaScript / TypeScript:**

```javascript
const display = result.params?.display;
const label =
  display?.title ||
  display?.name ||
  display?.label ||
  result.params?.itemId;
```

**Kotlin:**

```kotlin
val display = result.params["display"] as? Map<*, *>
val label =
    (display?.get("title") as? String)
        ?: (display?.get("name") as? String)
        ?: (display?.get("label") as? String)
        ?: (result.params["itemId"] as? String)
        ?: ""
```

**Swift:**

```swift
let display = result.params["display"] as? [String: Any]
let label =
    (display?["title"] as? String) ??
    (display?["name"] as? String) ??
    (display?["label"] as? String) ??
    (result.params["itemId"] as? String) ??
    ""
```

### 11.4 Backward Compatibility

- `params.itemId` is unchanged. All existing integrations continue to work without modification.
- `params.display` is optional and additive. If `displayFields` is not configured in your catalog mapping, `params.display` will not be present.
- Applications that do not need richer presentation may ignore `params.display` entirely.

---

## 12. Debugging

```kotlin
Log.d("NAVOICE", "your message")
```

In Android Studio **Logcat**, filter by tag: **`NAVOICE`**.

Enable SDK verbose logging with `.debug(true)` on `NavoiceConfig.Builder` (see section 5).

---

## 13. Integration checklist

- [ ] Copy **`navoice-sdk-release.aar`** to **`app/libs/`**
- [ ] Add **`implementation(files("libs/navoice-sdk-release.aar"))`** and required Maven dependencies
- [ ] Add **`INTERNET`** and **`RECORD_AUDIO`** to the manifest
- [ ] Create **`AppNavoiceConfig.kt`** (not `NavoiceConfig.kt`)
- [ ] Initialize **`Navoice`** with **`NavoiceConfig.Builder(this)`** and your publishable key
- [ ] Add **`app/src/main/assets/spec.json`**
- [ ] Set **`navoice.specProvider`** (assets → `JSONObject` → `Map`)
- [ ] Add JSON **`toMap` / `toList`** helpers
- [ ] Add microphone (or other) UI; call **`requestVoicePermission`**, **`startVoice`**, **`stopVoice`**
- [ ] Forward **`onRequestPermissionsResult`** for **`LocalSpeechRecognizer.REQUEST_RECORD_AUDIO`**
- [ ] Set **`navoice.onResult`** and implement **`navigateTo`** (and Present/ShowChoices if needed)

---

## Demo application: Navoice-MyCity

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

### Configuring the demo app

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

### License validation

Navoice validates the application using:

- Publishable key
- Android package name (applicationId)

If validation fails:

- Voice features are disabled
- Text routing is disabled
- UI remains functional

Ensure your package name is registered in the Navoice Portal under Allowed Identifiers.

---

## Android Views / XML integration

Navoice works with Activities, Fragments, and XML layouts. Initialize in **`onCreate`**, reuse the same **`AppNavoiceConfig`**, **`specProvider`**, and **`onResult`** pattern as above.

### Initialize in `Activity`

```kotlin
import android.util.Log
import io.navoice.sdk.Navoice
import io.navoice.sdk.NavoiceConfig
import io.navoice.sdk.NavoiceSTTConfig
import io.navoice.sdk.model.NavoiceResult
import org.json.JSONObject
import io.navoice.sdk.stt.LocalSpeechRecognizer

class MainActivity : AppCompatActivity() {

    private lateinit var navoice: Navoice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val config = NavoiceConfig.Builder(this)
            .publishableKey(AppNavoiceConfig.PUBLISHABLE_KEY)
            .identifier(packageName)
            .locale("en-US")
            .sttConfig(NavoiceSTTConfig.hybrid())
            .build(applicationId = packageName)

        navoice = Navoice(config)

        navoice.specProvider = suspend {
            val json = assets.open("spec.json").bufferedReader().use { it.readText() }
            JSONObject(json).toMap()
        }

        navoice.onResult = { result ->
            when (result) {
                is NavoiceResult.Execute -> navigateTo(result.screenId, result.params)
                is NavoiceResult.Unsupported -> Log.d("NAVOICE", result.say)
                is NavoiceResult.Present ->
                    Log.d("NAVOICE", "Present presentationId=${result.presentationId} (add UI if your spec uses present)")
                is NavoiceResult.ShowChoices ->
                    Log.d("NAVOICE", "ShowChoices say=${result.say} count=${result.choices.size} (add UI if your spec uses choices)")
            }
        }
    }

      override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LocalSpeechRecognizer.REQUEST_RECORD_AUDIO) {
            navoice.onRequestPermissionsResult(requestCode, grantResults)
        }
    }

    private fun navigateTo(screenId: String, params: Map<String, String>) {
        when (screenId) {
            "guitar" -> Log.d("NAVOICE", "guitar params=$params")
            "drums" -> Log.d("NAVOICE", "drums params=$params")
            "recorder" -> Log.d("NAVOICE", "recorder params=$params")
            "violin" -> Log.d("NAVOICE", "violin params=$params")
            else -> Log.d("NAVOICE", "Unhandled screenId=$screenId params=$params")
        }
        // Replace Log.d branches with startActivity(...), Fragment transactions, or NavController.navigate(...)
    }
}
```

Add the same **`JSONObject.toMap()` / `JSONArray.toList()`** private helpers used in **Full MainActivity Example** (bottom of the file or shared **`JsonSpec.kt`**).

### Start / stop voice (Views)

```kotlin
navoice.requestVoicePermission(this) { granted ->
    if (granted) navoice.startVoice()
}
// …
navoice.stopVoice()
```

Call **`navoice.stopVoice()`** from **`onPause()`** as in **Full MainActivity Example** when the user leaves the screen.

`Present` / `ShowChoices` are logged in the **`onResult`** block above; add real UI when your spec returns those types.

---

## Errors (quick reference)

| Situation | Typical outcome |
|-----------|-------------------|
| Empty / missing spec | `Unsupported` mentioning spec / `specProvider` |
| License not valid | `Unsupported` about license / subscription |
| Empty text in `route` | `Unsupported`: “Please type or say something” |
| Network / parse errors | `Unsupported` with error text; `routeAndCallback` forwards via `onResult` |

---

## Binary distribution

- Delivered as **`navoice-sdk-release.aar`**
- Host apps must add the runtime dependencies listed in section 3

---

## Support

support@navoice.io  

For issues, feature requests, or documentation, use your Navoice support channel or the Navoice Portal.
