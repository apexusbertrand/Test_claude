package com.apexus.storagelens.ui.screens.system

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexus.storagelens.data.delete.TrashRepository
import com.apexus.storagelens.data.scan.ScanManager
import com.apexus.storagelens.domain.model.StorageCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SystemStorageUiState(
    val systemBytes: Long? = null,
    val totalBytes: Long = 0,
    val inaccessibleDirectories: Int = 0,
    val appTrashBytes: Long = 0,
    val isSamsung: Boolean = false,
    /** Intents vers les écrans utiles, null si l'application correspondante n'est pas présente. */
    val deviceCareIntent: Intent? = null,
    val galleryIntent: Intent? = null,
    val filesIntent: Intent? = null,
    val storageSettingsIntent: Intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
)

@HiltViewModel
class SystemStorageViewModel @Inject constructor(
    @ApplicationContext context: Context,
    scanManager: ScanManager,
    trash: TrashRepository,
) : ViewModel() {

    private val pm = context.packageManager
    private fun launch(vararg packages: String): Intent? = packages.firstNotNullOfOrNull { pm.getLaunchIntentForPackage(it) }

    private val base = SystemStorageUiState(
        isSamsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true),
        // Entretien de l'appareil Samsung (contient le réglage RAM Plus).
        deviceCareIntent = launch(SAMSUNG_DEVICE_CARE),
        galleryIntent = launch(SAMSUNG_GALLERY, GOOGLE_PHOTOS),
        filesIntent = launch(SAMSUNG_MY_FILES, GOOGLE_FILES),
    )

    val state: StateFlow<SystemStorageUiState> = combine(scanManager.result, trash.items) { result, items ->
        base.copy(
            systemBytes = result?.categories?.firstOrNull { it.category == StorageCategory.SYSTEM }?.bytes,
            totalBytes = result?.volume?.totalBytes ?: 0,
            inaccessibleDirectories = result?.inaccessibleDirectories ?: 0,
            appTrashBytes = items.sumOf { it.size },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), base)

    companion object {
        const val SAMSUNG_DEVICE_CARE = "com.samsung.android.lool"
        const val SAMSUNG_GALLERY = "com.sec.android.gallery3d"
        const val GOOGLE_PHOTOS = "com.google.android.apps.photos"
        const val SAMSUNG_MY_FILES = "com.sec.android.app.myfiles"
        const val GOOGLE_FILES = "com.google.android.apps.nbu.files"
    }
}
