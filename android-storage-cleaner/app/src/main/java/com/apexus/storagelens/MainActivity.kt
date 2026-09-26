package com.apexus.storagelens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.activity.viewModels
import com.apexus.storagelens.data.prefs.AppSettings
import com.apexus.storagelens.data.prefs.SettingsRepository
import com.apexus.storagelens.ui.navigation.AppNavHost
import com.apexus.storagelens.ui.theme.StorageLensTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(settings: SettingsRepository) : ViewModel() {
    /** null tant que les réglages ne sont pas chargés (évite un flash de l'onboarding). */
    val settings: StateFlow<AppSettings?> = settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val current = settings ?: return@setContent
            // L'onboarding n'est choisi qu'au premier affichage : le NavHost garde ensuite son graphe.
            val showOnboarding = remember { !current.onboardingDone }
            StorageLensTheme(themeMode = current.themeMode) {
                AppNavHost(showOnboarding = showOnboarding)
            }
        }
    }
}
