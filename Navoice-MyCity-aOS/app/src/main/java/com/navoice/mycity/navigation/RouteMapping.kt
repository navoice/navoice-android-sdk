package com.navoice.mycity.navigation

/**
 * Maps spec.json screenIds to Android tab/index.
 * Matches iOS route(screenId:) in RootTabsView.
 */
object RouteMapping {
    const val SCREEN_EDUCATION = "education"
    const val SCREEN_EVENTS = "events"
    const val SCREEN_RECYCLE = "recycle"
    const val SCREEN_TAXES = "taxes"

    /**
     * Resolve screenId from spec to tab index (0-based).
     * Returns null if unknown.
     */
    fun screenIdToTabIndex(screenId: String?): Int? {
        val normalized = screenId?.substringBefore(".") ?: return null
        return when (normalized) {
            "education" -> 0
            "events" -> 1
            "recycle" -> 2
            "taxes" -> 3
            else -> null
        }
    }

    val validScreenIds: Set<String> = setOf(
        SCREEN_EDUCATION, SCREEN_EVENTS, SCREEN_RECYCLE, SCREEN_TAXES
    )
}
