package com.apexus.storagelens.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexus.storagelens.data.db.CleanupHistoryEntity
import com.apexus.storagelens.data.db.HistoryDao
import com.apexus.storagelens.data.db.ScanHistoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HistoryUiState(
    val scans: List<ScanHistoryEntity> = emptyList(),
    val cleanups: List<CleanupHistoryEntity> = emptyList(),
    val totalFreed: Long = 0,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(dao: HistoryDao) : ViewModel() {
    val state: StateFlow<HistoryUiState> = combine(dao.observeScans(), dao.observeCleanups(), dao.observeTotalFreed()) { scans, cleanups, total ->
        HistoryUiState(scans, cleanups, total)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())
}
