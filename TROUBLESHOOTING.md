# Navoice SDK — Production Troubleshooting Guide

This document provides production-level troubleshooting guidance for Android integrations.

---

## 🔐 License & Authentication Issues

### 1. License not active

**Symptom**

- SDK does not work
- Navigation does not execute

**Cause**

- `projects.license_status` is not `trial` or `active`

**Resolution**

- Verify the subscription or trial is active
- If recently activated and still failing, contact support

---

### 2. Invalid publishable_key

**Symptom**

- Backend returns: `Invalid publishable_key`

**Cause**

- The publishable key does not match the project in the Navoice Portal

**Resolution**

- Use the exact value from the Navoice Portal

---

### 3. Identifier is not allowed for this project

**Symptom**

- Backend returns: `Identifier is not allowed for this project`

**Cause**

- No matching row in `project_allowed_identifiers`

**Resolution**

- Register the Android application identifier in the portal
- It must match the app `applicationId`

Example `/api/license/validate` request body (Android):

```json
{
  "publishable_key": "YOUR_PUBLISHABLE_KEY",
  "platform": "android",
  "identifier": "com.yourcompany.yourapp"
}
```

---

## 🤖 Android AAR Integration Issues

### 4. AAR not loaded correctly

**Symptom**

- Build error
- SDK classes not found
- App crashes on startup

**Cause**

- `navoice-sdk-release.aar` is missing or not referenced correctly

**Resolution**

- Copy `navoice-sdk-release.aar` into `app/libs/`
- Add to `app/build.gradle.kts`:

```kotlin
implementation(files("libs/navoice-sdk-release.aar"))
```

If your project requires local AAR resolution, add this to your Gradle configuration:
```kotlin
repositories {
    flatDir {
        dirs("libs")
    }
}
```

---

### 5. Missing SDK runtime dependencies

**Symptom**

- `ClassNotFoundException`
- Missing Moshi / OkHttp / Coroutines classes

**Cause**

- Required runtime dependencies were not added to the host app

**Resolution**

Add these dependencies to `app/build.gradle.kts`:

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

### 6. Missing microphone permission

**Symptom**

- Voice input does not start
- Permission-related failures

**Cause**

- `RECORD_AUDIO` permission missing from `AndroidManifest.xml`

**Resolution**

Add to `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

---

## 🎤 STT Issues

### 7. STT fails

**Symptom**

- Voice recording starts but no result is returned
- Cloud STT fails

**Possible causes**

- Backend issue
- Invalid token
- Missing permission
- Network failure

**Resolution**

- Verify `RECORD_AUDIO` permission
- Verify license validation succeeds
- Verify protected endpoints receive a Bearer token
- If server transcription still fails, contact support

---

## 🧭 Navigation Issues

### 8. Voice works but no navigation

**Cause**

- Missing route mapping
- App does not handle returned `screenId`
- Spec does not contain a matching task

**Checklist**

- `spec.json` loads successfully
- App handles returned `NavoiceResult`
- `screenId` values match app navigation targets

---

## ✅ Production Validation Checklist

Before release, verify:

- `publishable_key` is correct
- `license_status` is `trial` or `active`
- `applicationId` is registered in the portal
- `/api/license/validate` returns a JWT
- Protected endpoints receive a Bearer token
- AAR added to `app/libs`
- Required dependencies added
- `RECORD_AUDIO` permission added
- Spec loaded successfully
- Route mapping matches spec `screenId`s

---

## 🏗 Architecture Overview

```
App (Android)
    ↓
Navoice SDK
    ↓
POST /api/license/validate
    ↓
License Gate (publishable_key + identifier + status)
    ↓
JWT Minted
    ↓
Protected APIs (/api/stt, /api/interpret)
    ↓
Navigation Result
```

---

### 9. Proguard / R8 issues

**Symptom**

- App crashes only in release build

**Cause**

- Classes removed by R8 / Proguard

**Resolution**

Add keep rules:

```
-keep class io.navoice.** { *; }
```
