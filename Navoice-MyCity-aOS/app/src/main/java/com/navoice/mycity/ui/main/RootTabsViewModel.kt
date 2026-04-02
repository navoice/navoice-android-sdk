package com.navoice.mycity.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navoice.mycity.R
import com.navoice.mycity.navigation.RouteMapping
import com.navoice.mycity.sdk.NavoiceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class MicState { SPEAK, LISTENING, THINKING }

class RootTabsViewModel : ViewModel() {

    var navoiceManager: NavoiceManager? = null

    private val _micState = MutableLiveData(MicState.SPEAK)
    val micState: LiveData<MicState> = _micState

    private val _badgeState = MutableLiveData<Pair<Boolean, Int?>>(false to null)
    val badgeState: LiveData<Pair<Boolean, Int?>> = _badgeState

    private var badgeJob: Job? = null

    fun onMicTapped() {
        when (_micState.value) {
            MicState.SPEAK -> {
                _micState.value = MicState.LISTENING
                navoiceManager?.startVoice()
            }
            MicState.LISTENING -> {
                _micState.value = MicState.THINKING
                navoiceManager?.stopVoice()
            }
            MicState.THINKING -> { /* no-op */ }
            null -> { }
        }
    }

    fun onResultReceived() {
        _micState.value = MicState.SPEAK
    }

    fun showBadge(colorRes: Int, durationMs: Long) {
        badgeJob?.cancel()
        _badgeState.value = true to colorRes
        badgeJob = viewModelScope.launch {
            delay(durationMs)
            _badgeState.value = false to null
        }
    }

    fun setSelectedTabFromDestination(destination: androidx.navigation.NavDestination) {
        // Tab selection is handled by BottomNav + NavController
    }
}
