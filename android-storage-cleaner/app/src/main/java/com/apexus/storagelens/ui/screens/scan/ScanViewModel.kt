package com.apexus.storagelens.ui.screens.scan

import androidx.lifecycle.ViewModel
import com.apexus.storagelens.data.scan.ScanManager
import com.apexus.storagelens.domain.model.ScanState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val scanManager: ScanManager,
) : ViewModel() {
    val state: StateFlow<ScanState> = scanManager.state

    fun cancel() = scanManager.cancel()

    fun retry() = scanManager.start()
}
