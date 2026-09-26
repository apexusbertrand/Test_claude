package com.apexus.storagelens.ui.screens.onboarding

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexus.storagelens.data.prefs.SettingsRepository
import com.apexus.storagelens.data.storage.PermissionChecker
import com.apexus.storagelens.data.storage.PermissionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    private val checker: PermissionChecker,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _status = MutableStateFlow(checker.status())
    val status: StateFlow<PermissionStatus> = _status.asStateFlow()

    fun refresh() {
        _status.value = checker.status()
    }

    fun allFilesIntents(): Array<Intent> = arrayOf(checker.allFilesAccessIntent(), checker.allFilesAccessFallbackIntent())
    fun usageIntent(): Intent = checker.usageAccessIntent()
    fun legacyPermissions(): Array<String> = checker.legacyStoragePermissions()

    fun finishOnboarding(then: () -> Unit) {
        viewModelScope.launch {
            settings.setOnboardingDone(true)
            then()
        }
    }
}
