package com.apexus.storagelens.ui.screens.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexus.storagelens.data.delete.TrashRepository
import com.apexus.storagelens.data.prefs.SettingsRepository
import com.apexus.storagelens.data.scan.ScanManager
import com.apexus.storagelens.domain.model.TrashItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrashUiState(
    val loading: Boolean = true,
    val items: List<TrashItem> = emptyList(),
    val retentionDays: Int = 7,
    val enabled: Boolean = true,
) {
    val totalBytes: Long get() = items.sumOf { it.size }
}

sealed interface TrashEvent {
    data class Restored(val count: Int) : TrashEvent
    data class Deleted(val count: Int) : TrashEvent
    data object Failed : TrashEvent
}

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val trash: TrashRepository,
    private val scanManager: ScanManager,
    settings: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<TrashUiState> = combine(trash.items, settings.settings) { items, s ->
        TrashUiState(loading = false, items = items, retentionDays = s.trashRetentionDays, enabled = s.trashEnabled)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrashUiState())

    private val _events = Channel<TrashEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun restore(item: TrashItem) {
        viewModelScope.launch {
            val ok = trash.restore(item.id).isSuccess
            if (ok) scanManager.markStale()
            _events.send(if (ok) TrashEvent.Restored(1) else TrashEvent.Failed)
        }
    }

    fun delete(item: TrashItem) {
        viewModelScope.launch {
            val ok = trash.deletePermanently(item.id)
            _events.send(if (ok) TrashEvent.Deleted(1) else TrashEvent.Failed)
        }
    }

    fun empty() {
        viewModelScope.launch { _events.send(TrashEvent.Deleted(trash.empty())) }
    }
}
