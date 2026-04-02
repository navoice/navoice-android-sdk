package com.navoice.mycity.sdk

sealed class NavoiceResult {
    data class Execute(val screenId: String) : NavoiceResult()

    /**
     * Present a UI sheet/dialog instead of navigating.
     */
    data class Present(
        val presentationId: String,
        val params: Map<String, Any?> = emptyMap(),
        val say: String? = null
    ) : NavoiceResult()

    data class ShowChoices(val choices: List<NavoiceChoice>) : NavoiceResult()
    data class Unsupported(val message: String) : NavoiceResult()
}

data class NavoiceChoice(
    val title: String,
    val screenId: String? = null
)