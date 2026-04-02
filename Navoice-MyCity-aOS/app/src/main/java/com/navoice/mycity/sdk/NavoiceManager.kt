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

    val hasPublishableKey: Boolean
        get() = com.navoice.mycity.BuildConfig.NAVOICE_PUBLISHABLE_KEY.isNotBlank()

    private var navoice: Navoice? = null
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    // --- Route context state (set by the app) ---
    private var currentScreenId: String = "home"
    private var lastScreenId: String? = null
    private var lastIntent: String? = null
    private var lastEntities: List<String>? = null

    init {

        if (!hasPublishableKey) {
            Log.w(TAG, "Navoice publishable key is missing. SDK will stay disabled.")
        } else {

            val config = NavoiceConfig.Builder()
                .context(context)
                .publishableKey(com.navoice.mycity.BuildConfig.NAVOICE_PUBLISHABLE_KEY)
                .baseUrl(com.navoice.mycity.BuildConfig.NAVOICE_BACKEND_BASE_URL)
                .locale(AppConfig.defaultLocale)
                .sttConfig(NavoiceSTTConfig.hybrid())
                .debug(true)
                .build(context.packageName)

            navoice = Navoice(config)

            tryAttachVoiceRouteContextProvider()

            navoice?.specProvider = {
                val spec = withContext(Dispatchers.IO) {
                    NavoiceSpecLoader.loadFromAssets(context, "mycity_spec")
                }
                Log.d("MyCitySpec", "spec size = ${spec.size}")
                spec
            }

            navoice?.onResult = { sdkResult ->

                if (com.navoice.mycity.BuildConfig.DEBUG) {
                    Log.d(TAG, "sdkResult type=${sdkResult::class.java.name}")
                    Log.d(TAG, "sdkResult value=$sdkResult")
                }

                when (sdkResult) {
                    is io.navoice.sdk.model.NavoiceResult.Execute -> {
                        setCurrentScreen(sdkResult.screenId)
                    }
                    else -> {}
                }

                onResult?.invoke(sdkToAppResult(sdkResult))
            }

            if (com.navoice.mycity.BuildConfig.DEBUG) {
                navoice?.setAuditCallback { event ->
                    when (event) {
                        is NavoiceAuditEvent.LicenseValidated ->
                            Log.d(TAG, "license validate success")
                        is NavoiceAuditEvent.LicenseValidateFailed ->
                            Log.d(TAG, "license validate failure")
                        else -> {}
                    }
                }
            }
        }
    }

    private fun tryAttachVoiceRouteContextProvider() {
        val sdk = navoice ?: return

        try {
            val setter = sdk.javaClass.methods.firstOrNull { it.name == "setVoiceRouteContextProvider" }
            if (setter != null) {
                setter.invoke(
                    sdk,
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
        if (!hasPublishableKey) {
            onResult?.invoke(
                NavoiceResult.Unsupported("You should add your publishable key to use voice navigation.")
            )
            return
        }
        navoice?.startVoice()
    }

    fun stopVoice() {
        navoice?.stopVoice()
    }

    fun routeText(text: String) {
        Log.d("MyCity", "[Typed] text='$text'")

        if (!hasPublishableKey) {
            onResult?.invoke(
                NavoiceResult.Unsupported("You should add your publishable key to use text routing.")
            )
            return
        }

        val sdk = navoice ?: run {
            onResult?.invoke(
                NavoiceResult.Unsupported("Navoice SDK is not configured.")
            )
            return
        }

        scope.launch {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) {
                onResult?.invoke(NavoiceResult.Unsupported("Type or say something 🙂"))
                return@launch
            }

            try {
                val direct = sdk.javaClass.methods.firstOrNull { m ->
                    m.name == "routeText" &&
                            m.parameterTypes.size == 1 &&
                            m.parameterTypes[0] == String::class.java
                }
                if (direct != null) {
                    direct.invoke(sdk, trimmed)
                    return@launch
                }

                val ctx = RouteContext(
                    locale = AppConfig.defaultLocale,
                    currentScreenId = currentScreenId,
                    lastIntent = lastIntent
                )
                val routeResponse = try {
                    sdk.routeText(trimmed, ctx)
                } catch (_: NoSuchMethodError) {
                    val cls = sdk.javaClass

                    val routeMethod = cls.methods.firstOrNull { m ->
                        m.name == "route" &&
                                m.parameterTypes.size == 1 &&
                                m.parameterTypes[0] == String::class.java
                    }
                    if (routeMethod != null) {
                        val res = routeMethod.invoke(sdk, trimmed)
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
                        interpretMethod.invoke(sdk, trimmed)
                        return@launch
                    }

                    throw IllegalStateException("No compatible text routing API found in SDK")
                }

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

                val sdkResult = try {
                    val mapMethod = sdk.javaClass.getDeclaredMethod(
                        "mapResponse",
                        io.navoice.sdk.model.RouteResponse::class.java
                    )
                    mapMethod.isAccessible = true
                    mapMethod.invoke(sdk, routeResponse) as io.navoice.sdk.model.NavoiceResult
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
        if (!hasPublishableKey) {
            callback(false)
            return
        }
        navoice?.requestVoicePermission(activity, callback) ?: callback(false)
    }

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        navoice?.onRequestPermissionsResult(requestCode, grantResults)
    }

    private fun sdkToAppResult(sdk: io.navoice.sdk.model.NavoiceResult): NavoiceResult {
        return when (sdk) {

            is io.navoice.sdk.model.NavoiceResult.Execute ->
                NavoiceResult.Execute(screenId = sdk.screenId)

            is io.navoice.sdk.model.NavoiceResult.Present ->
                NavoiceResult.Present(
                    presentationId = sdk.presentationId,
                    params = sdk.params.mapValues { it.value?.toString() ?: "" },
                    say = sdk.say
                )

            is io.navoice.sdk.model.NavoiceResult.ShowChoices ->
                NavoiceResult.Unsupported("choices not supported in app layer")

            is io.navoice.sdk.model.NavoiceResult.Unsupported ->
                NavoiceResult.Unsupported(sdk.say)
        }
    }

    companion object {
        private const val TAG = "NavoiceManager"
    }
}