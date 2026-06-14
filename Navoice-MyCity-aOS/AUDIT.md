# MyCity-aOS — AUDIT.md

## What was scanned

All Kotlin source files under `app/src/main/java/`. `app/src/main/AndroidManifest.xml`, `nav_graph.xml`, `mycity_spec.json`, `app/build.gradle.kts`, `settings.gradle.kts`, `README.md`, `.gitmodules`.

## What the project appears to do

An Android reference application demonstrating Navoice SDK integration for a municipal services app. Users navigate via voice to four feature tabs: Taxes, Recycle, Events, Education. The SDK is integrated via a pre-built `navoice-sdk.aar`. `NavoiceManager` handles SDK lifecycle, and `RouteMapping` translates Navoice `screenId` results to Android navigation destinations.

## Current architecture

- Single Activity (`RootTabsActivity`) + four Fragments (one per tab).
- MVVM: `RootTabsViewModel` manages SDK state and tab selection.
- Android Navigation Component with a bottom nav bar.
- `NavoiceManager` wraps SDK init, mic recording, and result handling.
- `RouteMapping` maps Navoice `screenId` strings to navigation action/destination IDs.
- Spec loaded from `assets/mycity_spec.json` at app startup.

## Main flows

1. **App launch** → `NavoiceManager.init()` → SDK validates license
2. **User taps mic** → `NavoiceManager.startListening()` → SDK records audio
3. **Recording complete** → SDK routes via local matcher → cloud fallback if needed
4. **Result "execute"** → `RouteMapping.navigate(screenId)` → Navigation Component navigates
5. **Result "show_choices"** → disambiguation UI shown (implementation — needs confirmation)

## API endpoints consumed (via SDK AAR)

| Endpoint | Purpose |
|---|---|
| `POST /api/license/validate` | License init |
| `POST /api/interpret` | Route text |
| `POST /api/stt` | Cloud STT |

## Dependencies

- `app/libs/navoice-sdk.aar` (pre-built, local)
- Android Jetpack: ViewModel, Navigation, Core KTX
- (Transitive from AAR): OkHttp, Moshi, Coroutines

## Missing documentation

- `README.md` exists — verify currency.
- No documented publishable key source or how to change it.
- No instructions for updating the AAR.
- `.gitmodules` exists but content is unclear — no documentation on what submodule was/is configured.
- `local.properties` must not be committed — verify `.gitignore` covers it.

## Duplicated logic

- `mycity_spec.json` is duplicated across MyCity-iOS, MyCity-Web, and MyCity-aOS. No single source of truth.
- `RouteMapping.kt` is an Android-specific layer that partially overlaps with the `routes` map in `MyCity-Web/app/navoiceInit.ts` — same concept, different implementation.

## Security concerns

- Publishable key in `AppConfig.kt` is hardcoded in source. This is acceptable (public key) but should be clearly documented.
- `RECORD_AUDIO` permission grants microphone access. The SDK sends audio to Navoice servers for cloud STT — this must be disclosed in the app's privacy policy.
- `local.properties` (containing `sdk.dir`) must not be committed. Verify `.gitignore`.

## Integration risks

- `navoice-sdk.aar` can become stale relative to SDK-aOS. If the AAR is not rebuilt after SDK changes, the demo shows outdated behavior.
- `RouteMapping.kt` must be updated when spec task IDs change — no compile-time check.
- `.gitmodules` suggests a submodule was or is configured. If the submodule is active but uninitialized, the project may have missing files.
- Android SpeechRecognizer availability varies by device/locale. Local STT may fail silently on some devices.

## Recommended next tasks

1. Investigate and document `.gitmodules` — is the submodule still needed?
2. Add README section on how to update the AAR and spec.
3. Automate AAR copy from SDK-aOS.
4. Sync `mycity_spec.json` across all three MyCity apps.
5. Verify `local.properties` is in `.gitignore`.
6. Add a comment in `AppConfig.kt` documenting the publishable key and where to change it.
