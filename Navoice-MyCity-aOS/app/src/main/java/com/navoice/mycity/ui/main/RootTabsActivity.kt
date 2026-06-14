package com.navoice.mycity.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import com.navoice.mycity.R
import com.navoice.mycity.databinding.ActivityRootTabsBinding
import com.navoice.mycity.navigation.RouteMapping
import com.navoice.mycity.sdk.NavoiceChoice
import com.navoice.mycity.sdk.NavoiceManager
import com.navoice.mycity.sdk.NavoiceResult
import androidx.appcompat.app.AlertDialog
import android.widget.Toast

class RootTabsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRootTabsBinding
    private lateinit var navoiceManager: NavoiceManager
    private val viewModel: RootTabsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRootTabsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navoiceManager = NavoiceManager(this)
        navoiceManager.onResult = ::handleNavoiceResult
        viewModel.navoiceManager = navoiceManager

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNav.setupWithNavController(navController)

        // Sync tab selection with nav destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            viewModel.setSelectedTabFromDestination(destination)
        }

        setupMicButton()
        setupTextButton()
        observeViewModel()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        navoiceManager.onRequestPermissionsResult(requestCode, grantResults)
    }

    private fun hasRecordAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

    private fun setupMicButton() {
        val micClickListener: () -> Unit = {
            when (viewModel.micState.value) {
                MicState.SPEAK -> {
                    if (!hasRecordAudioPermission()) {
                        navoiceManager.requestVoicePermission(this) { granted ->
                            if (granted) viewModel.onMicTapped()
                        }
                    } else {
                        viewModel.onMicTapped()
                    }
                }

                MicState.LISTENING -> {
                    viewModel.onMicTapped()
                }

                MicState.THINKING -> {
                    // no-op
                }

                null -> {
                    // no-op
                }
            }
        }

        binding.micButton.setOnClickListener { micClickListener() }
        binding.micButtonContainer.setOnClickListener { micClickListener() }
    }

    private fun setupTextButton() {
        val textClickListener: () -> Unit = {
            val input = EditText(this).apply {
                hint = "Type your request..."
            }

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Text command")
                .setView(input)
                .setPositiveButton("Send") { _, _ ->
                    val value = input.text?.toString()?.trim().orEmpty()
                    if (value.isNotEmpty()) {
                        navoiceManager.routeText(value)
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        binding.pencilButton.setOnClickListener { textClickListener() }
        binding.pencilButtonContainer.setOnClickListener { textClickListener() }
    }

    private fun observeViewModel() {
        viewModel.micState.observe(this) { state ->
            updateMicUI(state)
        }
        viewModel.badgeState.observe(this) { (show, colorRes) ->
            binding.micBadge.visibility =
                if (show) android.view.View.VISIBLE else android.view.View.GONE
            if (show && colorRes != null) {
                binding.micBadge.setBackgroundResource(
                    if (colorRes == R.color.badge_success) R.drawable.badge_success
                    else R.drawable.badge_error
                )
            }
        }
    }

    private fun updateMicUI(state: MicState) {
        val bgRes = when (state) {
            MicState.SPEAK -> R.color.mic_idle
            MicState.LISTENING -> R.color.mic_listening
            MicState.THINKING -> R.color.mic_thinking
        }
        binding.micButtonBg.setBackgroundColor(ContextCompat.getColor(this, bgRes))

        when (state) {
            MicState.SPEAK -> {
                binding.micButton.visibility = android.view.View.VISIBLE
                binding.micButton.setImageResource(R.drawable.ic_mic)
                binding.micProgress.visibility = android.view.View.GONE
                binding.micButton.contentDescription =
                    getString(R.string.mic_accessibility_speak)
            }

            MicState.LISTENING -> {
                binding.micButton.visibility = android.view.View.VISIBLE
                binding.micButton.setImageResource(R.drawable.ic_stop)
                binding.micProgress.visibility = android.view.View.GONE
                binding.micButton.contentDescription =
                    getString(R.string.mic_accessibility_listening)
            }

            MicState.THINKING -> {
                binding.micButton.visibility = android.view.View.GONE
                binding.micProgress.visibility = android.view.View.VISIBLE
                binding.micButton.contentDescription =
                    getString(R.string.mic_accessibility_thinking)
            }
        }
    }

    private fun handleNavoiceResult(result: NavoiceResult) {
        runOnUiThread {
            viewModel.onResultReceived()
            when (result) {
                is NavoiceResult.Execute -> {
                    viewModel.showBadge(R.color.badge_success, 1500L)
                    if (result.screenId == "catalogItemDetails") {
                        showCatalogItemToast(
                            result.params.mapValues { it.value?.toString() ?: "" }
                        )
                        return@runOnUiThread
                    }
                    val normalized = result.screenId.substringBefore(".") // "recycle.home" -> "recycle"
                    val tabIndex = RouteMapping.screenIdToTabIndex(normalized)
                    if (tabIndex != null) {
                        navigateToTab(tabIndex)
                    } else {
                        viewModel.showBadge(R.color.badge_error, 5000L)
                    }
                }

                is NavoiceResult.Present -> {
                    viewModel.showBadge(R.color.badge_success, 1500L)

                    showPresentationDialog(
                        presentationId = result.presentationId,
                        params = result.params.mapValues { it.value?.toString() ?: "" },
                        say = result.say
                    )
                }

                is NavoiceResult.ShowChoices -> {
                    viewModel.showBadge(R.color.badge_error, 5000L)
                    showChoicesBottomSheet(result.choices)
                }

                is NavoiceResult.Unsupported -> {
                    viewModel.showBadge(R.color.badge_error, 5000L)
                    Toast.makeText(this, "לא מצאתי", Toast.LENGTH_LONG).show()
                }

                is NavoiceResult.PlanRestricted -> {
                    viewModel.showBadge(R.color.badge_error, 5000L)
                }
            }
        }
    }

    private fun showPresentationDialog(
        presentationId: String,
        params: Map<String, String>,
        say: String?
    ) {
        val value = resolvePresentationValue(presentationId, params) ?: "N/A"

        AlertDialog.Builder(this)
            .setTitle(presentationId.replace("_", " "))
            .setMessage(value)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showCatalogItemToast(params: Map<String, String>) {
        val title =
            params["title"]
                ?: params["name"]
                ?: params["movieTitle"]
                ?: params["label"]
                ?: "Item found"

        Toast.makeText(this, title, Toast.LENGTH_LONG).show()
    }

    private fun resolvePresentationValue(presentationId: String, params: Map<String, String>): String? {
        // If you later pass a value via params (from the server or the app), wire it here.

        return when (presentationId) {
            "id_number" -> "123456789"
            "subscriber_number" -> "SUB-45821"
            "passport_number" -> "A9876543"
            else -> null
        }
    }

    private fun showCatalogItemDialog(params: Map<String, String>) {
        val itemId = params["itemId"]
            ?: params["movieId"]
            ?: params["id"]
            ?: "Unknown"

        AlertDialog.Builder(this)
            .setTitle("Item Found")
            .setMessage("Catalog item ID: $itemId")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun navigateToTab(tabIndex: Int) {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val destId = when (tabIndex) {
            0 -> R.id.educationFragment
            1 -> R.id.eventsFragment
            2 -> R.id.recycleFragment
            3 -> R.id.taxesFragment
            else -> return
        }

        val navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setPopUpTo(navController.graph.startDestinationId, true, true)
            .build()

        navController.navigate(destId, null, navOptions)
    }

    private fun showChoicesBottomSheet(choices: List<NavoiceChoice>) {
        if (choices.isEmpty()) return
        val items = choices.map { it.title }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.choices_title)
            .setItems(items) { _, which ->
                val choice = choices.getOrNull(which) ?: return@setItems

                // 🔥 NEW — catalog support
                if (choice.screenId == "catalogItemDetails") {
                    showCatalogItemDialog(
                        choice.params.mapValues { it.value?.toString() ?: "" }
                    )
                    return@setItems
                }

                // fallback רגיל
                choice.screenId?.let { screenId ->
                    RouteMapping.screenIdToTabIndex(screenId)?.let { index ->
                        navigateToTab(index)
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}