package com.navoice.mycity.sdk

sealed class NavoiceResult {
    data class Execute(
        val screenId: String,
        val params: Map<String, Any?> = emptyMap(),
        val say: String?,
        val confidence: Double?
    ) : NavoiceResult()
    data class Present(
        val presentationId: String,
        val params: Map<String, Any?> = emptyMap(),
        val say: String? = null
    ) : NavoiceResult()

    data class ShowChoices(val choices: List<NavoiceChoice>) : NavoiceResult()
    data class Unsupported(val message: String) : NavoiceResult()
    data class PlanRestricted(val reason: String, val requiredPlan: String) : NavoiceResult()
}

data class NavoiceChoice(
    val title: String,
    val screenId: String?,
    val params: Map<String, Any?> = emptyMap()
)