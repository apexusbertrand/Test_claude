package com.apexus.storagelens.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexus.storagelens.data.prefs.AppSettings
import com.apexus.storagelens.data.prefs.ScheduleMode
import com.apexus.storagelens.data.prefs.SettingsRepository
import com.apexus.storagelens.data.prefs.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {
    val settings: StateFlow<AppSettings?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun edit(block: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch { repository.block() }
    }

    fun setLargeFileThreshold(mb: Int) = edit { setLargeFileThresholdMb(mb) }
    fun setOldFileMonths(months: Int) = edit { setOldFileMonths(months) }
    fun setTrashEnabled(enabled: Boolean) = edit { setTrashEnabled(enabled) }
    fun setRetentionDays(days: Int) = edit { setTrashRetentionDays(days) }
    fun setSchedule(mode: ScheduleMode) = edit { setScheduleMode(mode) }
    fun setNotifyThreshold(mb: Int) = edit { setNotifyThresholdMb(mb) }
    fun setShowHidden(show: Boolean) = edit { setShowHiddenFiles(show) }
    fun setRootMode(enabled: Boolean) = edit { setAdvancedRootMode(enabled) }
    fun setTheme(mode: ThemeMode) = edit { setThemeMode(mode) }
    fun addExclusion(path: String) {
        if (path.isNotBlank()) edit { addExclusion(path.trim()) }
    }
    fun removeExclusion(path: String) = edit { removeExclusion(path) }
}
