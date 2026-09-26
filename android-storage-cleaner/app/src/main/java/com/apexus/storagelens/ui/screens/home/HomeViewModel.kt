package com.apexus.storagelens.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexus.storagelens.data.db.HistoryDao
import com.apexus.storagelens.data.db.ScanHistoryEntity
import com.apexus.storagelens.data.scan.ScanManager
import com.apexus.storagelens.data.storage.PermissionChecker
import com.apexus.storagelens.data.storage.PermissionStatus
import com.apexus.storagelens.data.storage.StorageVolumesRepository
import com.apexus.storagelens.domain.model.ScanResult
import com.apexus.storagelens.domain.model.ScanState
import com.apexus.storagelens.domain.model.StorageVolumeInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    val volumes: List<StorageVolumeInfo> = emptyList(),
    val selectedVolumeId: String? = null,
    val permissions: PermissionStatus? = null,
    val result: ScanResult? = null,
    val stale: Boolean = false,
    val scanRunning: Boolean = false,
    val lastScan: ScanHistoryEntity? = null,
) {
    val selectedVolume: StorageVolumeInfo?
        get() = volumes.firstOrNull { it.id == selectedVolumeId } ?: volumes.firstOrNull()

    /** Le résultat affiché ne vaut que pour le volume sélectionné. */
    val resultForSelected: ScanResult?
        get() = result?.takeIf { it.volume.id == selectedVolume?.id }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val volumesRepository: StorageVolumesRepository,
    private val scanManager: ScanManager,
    private val permissionChecker: PermissionChecker,
    historyDao: HistoryDao,
) : ViewModel() {

    private val local = MutableStateFlow(HomeUiState())

    val state: StateFlow<HomeUiState> = combine(
        local,
        scanManager.result,
        scanManager.stale,
        scanManager.state,
        historyDao.observeLastScan(),
    ) { base, result, stale, scanState, lastScan ->
        base.copy(result = result, stale = stale, scanRunning = scanState is ScanState.Running, lastScan = lastScan)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun refresh() {
        viewModelScope.launch {
            val volumes = volumesRepository.volumes()
            local.update {
                it.copy(
                    loading = false,
                    volumes = volumes,
                    selectedVolumeId = it.selectedVolumeId ?: volumes.firstOrNull()?.id,
                    permissions = permissionChecker.status(),
                )
            }
        }
    }

    fun selectVolume(id: String) = local.update { it.copy(selectedVolumeId = id) }

    fun startScan() = scanManager.start(state.value.selectedVolume?.id)
}
