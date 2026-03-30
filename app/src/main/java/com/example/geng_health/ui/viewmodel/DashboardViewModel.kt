package com.example.geng_health.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geng_health.data.UsageStatsHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _screenTimeMillis = MutableStateFlow(0L)
    val screenTimeMillis = _screenTimeMillis.asStateFlow()

    private val _hasPermission = MutableStateFlow(UsageStatsHelper.hasUsageStatsPermission(application))
    val hasPermission = _hasPermission.asStateFlow()

    private var timerJob: Job? = null
    private var lastSystemUpdateMillis = 0L
    private var sessionStartMillis = 0L

    fun checkPermission() {
        _hasPermission.value = UsageStatsHelper.hasUsageStatsPermission(getApplication())
        if (_hasPermission.value) {
            startTracking()
        }
    }

    private fun startTracking() {
        timerJob?.cancel()
        
        // Initial fetch from system
        lastSystemUpdateMillis = UsageStatsHelper.getTodayTotalScreenTime(getApplication())
        sessionStartMillis = System.currentTimeMillis()
        _screenTimeMillis.value = lastSystemUpdateMillis

        timerJob = viewModelScope.launch(Dispatchers.Default) {
            var updateCounter = 0
            while (isActive) {
                val currentSessionTime = System.currentTimeMillis() - sessionStartMillis
                _screenTimeMillis.value = lastSystemUpdateMillis + currentSessionTime
                
                // Sync with system every 30 seconds to adjust for other apps
                updateCounter++
                if (updateCounter >= 30) {
                    val systemTime = UsageStatsHelper.getTodayTotalScreenTime(getApplication())
                    // If system time is larger than our estimate (user was using other apps?), sync up
                    if (systemTime > lastSystemUpdateMillis + currentSessionTime) {
                        lastSystemUpdateMillis = systemTime
                        sessionStartMillis = System.currentTimeMillis()
                    }
                    updateCounter = 0
                }
                
                delay(1000) // Update UI every second
            }
        }
    }

    fun formatTime(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = (TimeUnit.MILLISECONDS.toMinutes(millis) % 60)
        val seconds = (TimeUnit.MILLISECONDS.toSeconds(millis) % 60)
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
