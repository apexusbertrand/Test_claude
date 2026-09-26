package com.apexus.storagelens.ui.screens.apps

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.apexus.storagelens.data.apps.AppStorageRepository
import com.apexus.storagelens.data.storage.PermissionChecker
import com.apexus.storagelens.domain.model.AppStorageInfo
import com.apexus.storagelens.ui.navigation.AppsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class AppSort { TOTAL, CACHE, NAME }

enum class UsageFilter(val days: Int) { ALL(0), UNUSED_30(30), UNUSED_90(90) }

data class AppsUiState(
    val loading: Boolean = true,
    val usageAccess: Boolean = true,
    val apps: List<AppStorageInfo> = emptyList(),
    val sort: AppSort = AppSort.TOTAL,
    val filter: UsageFilter = UsageFilter.ALL,
    val showSystem: Boolean = false,
) {
    val visible: List<AppStorageInfo>
        get() {
            val now = System.currentTimeMillis()
            val filtered = apps.filter { app ->
                (showSystem || !app.isSystemApp) &&
                    (filter == UsageFilter.ALL || (app.lastUsedMillis ?: 0L) < now - TimeUnit.DAYS.toMillis(filter.days.toLong()))
            }
            return when (sort) {
                AppSort.TOTAL -> filtered.sortedByDescending { it.totalBytes }
                AppSort.CACHE -> filtered.sortedByDescending { it.cacheBytes }
                AppSort.NAME -> filtered.sortedBy { it.label.lowercase() }
            }
        }
    val totalCache: Long get() = apps.sumOf { it.cacheBytes }
}

@HiltViewModel
class AppsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AppStorageRepository,
    private val permissions: PermissionChecker,
) : ViewModel() {

    private val route = runCatching { savedStateHandle.toRoute<AppsRoute>() }.getOrDefault(AppsRoute())
    private val _state = MutableStateFlow(AppsUiState(sort = if (route.sortByCache) AppSort.CACHE else AppSort.TOTAL))
    val state: StateFlow<AppsUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val usage = permissions.hasUsageAccess()
            val apps = repository.appsWithStorage()
            _state.update { it.copy(loading = false, usageAccess = usage, apps = apps) }
        }
    }

    fun setSort(sort: AppSort) = _state.update { it.copy(sort = sort) }
    fun setFilter(filter: UsageFilter) = _state.update { it.copy(filter = filter) }
    fun setShowSystem(show: Boolean) = _state.update { it.copy(showSystem = show) }
}
