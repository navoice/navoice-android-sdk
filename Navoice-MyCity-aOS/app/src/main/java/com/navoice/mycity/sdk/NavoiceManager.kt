package com.navoice.mycity.sdk

import android.app.Activity
import android.content.Context
import android.util.Log
import com.navoice.mycity.config.AppConfig
import io.navoice.sdk.Navoice
import io.navoice.sdk.NavoiceConfig
import io.navoice.sdk.NavoiceSpecLoader
import io.navoice.sdk.NavoiceSTTConfig
import io.navoice.sdk.model.NavoiceAuditEvent
import io.navoice.sdk.model.RouteContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NavoiceManager(private val context: Context) {

    var onResult: ((NavoiceResult) -> Unit)? = null

    private val navoice: Navoice
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    // --- Route context state (set by the app) ---
    private var currentScreenId: String = "home"
    private var lastScreenId: String? = null
    private var lastIntent: String? = null
    private var lastEntities: List<String>? = null

    init {
        val config = NavoiceConfig.Builder()
            .context(context)
            .publishableKey(com.navoice.mycity.BuildConfig.NAVOICE_PUBLISHABLE_KEY)
            .baseUrl(com.navoice.mycity.BuildConfig.NAVOICE_BACKEND_BASE_URL)
            .locale(AppConfig.defaultLocale).locale("he-IL")
            .sttConfig(NavoiceSTTConfig.hybrid())
            .debug(true)
            .build(context.packageName)

        navoice = Navoice(config)

        // Provide voice route context to SDK (if SDK supports it).
        // IMPORTANT: We set it via reflection so the app compiles even if SDK API isn't updated in this build.
        tryAttachVoiceRouteContextProvider()

        // Required: load spec from app assets so /api/interpret can run
        navoice.specProvider = {
            val spec = withContext(Dispatchers.IO) {
                NavoiceSpecLoader.loadFromAssets(context, "mycity_spec")
            }
            Log.d("MyCitySpec", "spec size = ${spec.size}")
            spec
        }

        navoice.onResult = { sdkResult ->
            if (com.navoice.mycity.BuildConfig.DEBUG) {
                Log.d(TAG, "sdkResult type=${sdkResult::class.java.name}")
                Log.d(TAG, "sdkResult value=$sdkResult")
                when (sdkResult) {
                    is io.navoice.sdk.model.NavoiceResult.Execute ->
                        Log.d(TAG, "interpret response mode=execute screenId=${sdkResult.screenId}")
                    is io.navoice.sdk.model.NavoiceResult.ShowChoices ->
                        Log.d(TAG, "interpret response mode=show_choices count=${sdkResult.choices.size}")
                    is io.navoice.sdk.model.NavoiceResult.Unsupported ->
                        Log.d(TAG, "interpret response mode=unsupported")
                    is io.navoice.sdk.model.NavoiceResult.Present ->
                        Log.d(TAG, "interpret response mode=present presentationId=${sdkResult.presentationId}")
                    is io.navoice.sdk.model.NavoiceResult.PlanRestricted ->
                        Log.d(TAG, "interpret response mode=plan_restricted reason=${sdkResult.reason} requiredPlan=${sdkResult.requiredPlan}")
                }
            }

            // Optional: update session memory from the final SDK decision (simple + safe)
            when (sdkResult) {
                is io.navoice.sdk.model.NavoiceResult.Execute -> {
                    setCurrentScreen(sdkResult.screenId)
                }
                else -> { /* no-op */ }
            }

            onResult?.invoke(sdkToAppResult(sdkResult))
        }

        // Debug-only: license and flow (never log JWT or raw audio)
        if (com.navoice.mycity.BuildConfig.DEBUG) {
            navoice.setAuditCallback { event ->
                when (event) {
                    is NavoiceAuditEvent.LicenseValidated ->
                        Log.d(TAG, "license validate success (projectId=${event.projectId}, expiresAt=${event.expiresAtISO})")
                    is NavoiceAuditEvent.LicenseValidateFailed ->
                        Log.d(TAG, "license validate failure: ${event.message}")
                    is NavoiceAuditEvent.LicenseValidateRequested ->
                        Log.d(TAG, "license validate requested (identifier=${event.identifier.take(20)}...)")
                    else -> { /* other events already logged by SDK when debug=true */ }
                }
            }
        }
    }

    /**
     * Attach provider if SDK exposes setVoiceRouteContextProvider / voiceRouteContextProvider.
     * Uses reflection to avoid compile-time dependency on a specific SDK revision.
     */
    private fun tryAttachVoiceRouteContextProvider() {
        try {
            val setter = navoice.javaClass.methods.firstOrNull { it.name == "setVoiceRouteContextProvider" }
            if (setter != null) {
                setter.invoke(
                    navoice,
                    {
                        RouteContext(
                            locale = AppConfig.defaultLocale,
                            currentScreenId = currentScreenId,
                            lastIntent = lastIntent
                        )
                    }
                )
                Log.d(TAG, "voiceRouteContextProvider attached")
            } else {
                Log.d(TAG, "voiceRouteContextProvider not available in this SDK build")
            }
        } catch (t: Throwable) {
            Log.d(TAG, "voiceRouteContextProvider attach failed: ${t.message}")
        }
    }

    /**
     * App should call this on every screen change / navigation.
     */
    fun setCurrentScreen(screenId: String) {
        if (screenId.isBlank()) return
        if (screenId == currentScreenId) return
        lastScreenId = currentScreenId
        currentScreenId = screenId
    }

    fun updateSessionContext(
        lastIntent: String? = null,
        lastEntities: List<String>? = null
    ) {
        if (!lastIntent.isNullOrBlank()) this.lastIntent = lastIntent
        if (lastEntities != null) this.lastEntities = lastEntities
    }

    fun startVoice() {
        navoice.startVoice()
    }

    fun stopVoice() {
        navoice.stopVoice()
    }

    fun routeText(text: String) {
        Log.d("MyCity", "[Typed] text='$text'")
        scope.launch {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) {
                onResult?.invoke(NavoiceResult.Unsupported("Type or say something 🙂"))
                return@launch
            }

            try {
                // 1) Prefer a direct SDK text routing API that takes a single String and handles callbacks itself.
                val direct = navoice.javaClass.methods.firstOrNull { m ->
                    m.name == "routeText" &&
                            m.parameterTypes.size == 1 &&
                            m.parameterTypes[0] == String::class.java
                }
                if (direct != null) {
                    direct.invoke(navoice, trimmed)
                    return@launch
                }

                // 2) Fallback: call the unified text pipeline (routeText(text, context)).
                val ctx = RouteContext(
                    locale = AppConfig.defaultLocale,
                    currentScreenId = currentScreenId,
                    lastIntent = lastIntent
                )
                val routeResponse = try {
                    navoice.routeText(trimmed, ctx)
                } catch (_: NoSuchMethodError) {
                    // 3) Very old SDK: try route(String) or interpretText(String) if available.
                    val cls = navoice.javaClass

                    val routeMethod = cls.methods.firstOrNull { m ->
                        m.name == "route" &&
                                m.parameterTypes.size == 1 &&
                                m.parameterTypes[0] == String::class.java
                    }
                    if (routeMethod != null) {
                        val res = routeMethod.invoke(navoice, trimmed)
                        if (res is io.navoice.sdk.model.NavoiceResult) {
                            onResult?.invoke(sdkToAppResult(res))
                            return@launch
                        }
                    }

                    val interpretMethod = cls.methods.firstOrNull { m ->
                        m.name == "interpretText" &&
                                m.parameterTypes.size == 1 &&
                                m.parameterTypes[0] == String::class.java
                    }
                    if (interpretMethod != null) {
                        interpretMethod.invoke(navoice, trimmed)
                        return@launch
                    }

                    throw IllegalStateException("No compatible text routing API found in SDK")
                }

                // If the unified router says "present", surface a Present result directly (no navigation).
                val decisionKind = routeResponse.decisionKind ?: routeResponse.kind
                if (decisionKind == "present" || routeResponse.presentationId != null) {
                    val presId = routeResponse.presentationId
                        ?: routeResponse.screenId
                        ?: routeResponse.taskId
                        ?: "unknown"

                    val paramsAny: Map<String, Any?> =
                        (routeResponse.params ?: emptyMap()).mapValues { it.value }

                    val say = routeResponse.say
                    onResult?.invoke(
                        NavoiceResult.Present(
                            presentationId = presId,
                            params = paramsAny,
                            say = say
                        )
                    )
                    return@launch
                }

                // Otherwise map to the SDK's sealed NavoiceResult and then to app-level result.
                val sdkResult = try {
                    val mapMethod = navoice.javaClass.getDeclaredMethod(
                        "mapResponse",
                        io.navoice.sdk.model.RouteResponse::class.java
                    )
                    mapMethod.isAccessible = true
                    mapMethod.invoke(navoice, routeResponse) as io.navoice.sdk.model.NavoiceResult
                } catch (t: Throwable) {
                    io.navoice.sdk.model.NavoiceResult.Unsupported(
                        routeResponse.say ?: "Text routing failed"
                    )
                }

                if (sdkResult is io.navoice.sdk.model.NavoiceResult.Execute) {
                    setCurrentScreen(sdkResult.screenId)
                }

                onResult?.invoke(sdkToAppResult(sdkResult))
            } catch (t: Throwable) {
                onResult?.invoke(
                    NavoiceResult.Unsupported(
                        t.message ?: "Text routing failed"
                    )
                )
            }
        }
    }

    fun requestVoicePermission(activity: Activity, callback: (Boolean) -> Unit) {
        navoice.requestVoicePermission(activity, callback)
    }

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        navoice.onRequestPermissionsResult(requestCode, grantResults)
    }


    private fun sdkToAppResult(sdk: io.navoice.sdk.model.NavoiceResult): NavoiceResult {
        return when (sdk) {

            is io.navoice.sdk.model.NavoiceResult.Execute ->
                NavoiceResult.Execute(
                    screenId = sdk.screenId,
                    params = sdk.params,
                    say = sdk.say,
                    confidence = sdk.confidence
                )

            is io.navoice.sdk.model.NavoiceResult.Present ->
                NavoiceResult.Present(
                    presentationId = sdk.presentationId,
                    params = sdk.params.mapValues { it.value?.toString() ?: "" },
                    say = sdk.say
                )

            is io.navoice.sdk.model.NavoiceResult.ShowChoices ->
                NavoiceResult.ShowChoices(
                    choices = sdk.choices.map { c ->
                        NavoiceChoice(
                            title = c.title,
                            screenId = c.screenId ?: c.taskId,
                            params = c.params?.mapValues { it.value?.toString() ?: "" } ?: emptyMap()
                        )
                    }
                )

            is io.navoice.sdk.model.NavoiceResult.Unsupported ->
                NavoiceResult.Unsupported(sdk.say)

            is io.navoice.sdk.model.NavoiceResult.PlanRestricted ->
                NavoiceResult.PlanRestricted(reason = sdk.reason, requiredPlan = sdk.requiredPlan)
        }
    }

    companion object {
        private const val TAG = "NavoiceManager"
    }
}