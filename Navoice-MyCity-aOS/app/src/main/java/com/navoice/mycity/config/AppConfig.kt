package com.navoice.mycity.config

import com.navoice.mycity.BuildConfig

/**
 * Central app config - no hardcoded identifiers, keys, or URLs in app code.
 * Values come from BuildConfig (populated from gradle.properties / local.properties).
 */
object AppConfig {
    val publishableKey: String get() = BuildConfig.NAVOICE_PUBLISHABLE_KEY
    val backendBaseUrl: String get() = BuildConfig.NAVOICE_BACKEND_BASE_URL
    const val defaultLocale: String = "en-US"
    const val sttConfig: String = "local"  // local first, cloud fallback (matches iOS)
}
