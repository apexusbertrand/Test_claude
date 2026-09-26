package com.apexus.storagelens.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class ScheduleMode { OFF, WEEKLY, MONTHLY }

data class AppSettings(
    val onboardingDone: Boolean = false,
    val largeFileThresholdMb: Int = 100,
    val oldFileMonths: Int = 6,
    val trashEnabled: Boolean = true,
    val trashRetentionDays: Int = 7,
    val scheduleMode: ScheduleMode = ScheduleMode.OFF,
    val notifyThresholdMb: Int = 500,
    val showHiddenFiles: Boolean = true,
    val advancedRootMode: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val exclusions: Set<String> = emptySet(),
) {
    val largeFileThresholdBytes: Long get() = largeFileThresholdMb * 1024L * 1024L
    val oldFileAgeMillis: Long get() = oldFileMonths * 30L * 24 * 3600 * 1000
    val notifyThresholdBytes: Long get() = notifyThresholdMb * 1024L * 1024L
}

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val LARGE_MB = intPreferencesKey("large_file_mb")
        val OLD_MONTHS = intPreferencesKey("old_file_months")
        val TRASH = booleanPreferencesKey("trash_enabled")
        val RETENTION = intPreferencesKey("trash_retention_days")
        val SCHEDULE = stringPreferencesKey("schedule_mode")
        val NOTIFY_MB = intPreferencesKey("notify_threshold_mb")
        val HIDDEN = booleanPreferencesKey("show_hidden")
        val ROOT = booleanPreferencesKey("advanced_root")
        val THEME = stringPreferencesKey("theme_mode")
        val EXCLUSIONS = stringSetPreferencesKey("exclusions")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { p ->
        val defaults = AppSettings()
        AppSettings(
            onboardingDone = p[Keys.ONBOARDING] ?: defaults.onboardingDone,
            largeFileThresholdMb = p[Keys.LARGE_MB] ?: defaults.largeFileThresholdMb,
            oldFileMonths = p[Keys.OLD_MONTHS] ?: defaults.oldFileMonths,
            trashEnabled = p[Keys.TRASH] ?: defaults.trashEnabled,
            trashRetentionDays = p[Keys.RETENTION] ?: defaults.trashRetentionDays,
            scheduleMode = p[Keys.SCHEDULE]?.let { runCatching { ScheduleMode.valueOf(it) }.getOrNull() } ?: defaults.scheduleMode,
            notifyThresholdMb = p[Keys.NOTIFY_MB] ?: defaults.notifyThresholdMb,
            showHiddenFiles = p[Keys.HIDDEN] ?: defaults.showHiddenFiles,
            advancedRootMode = p[Keys.ROOT] ?: defaults.advancedRootMode,
            themeMode = p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: defaults.themeMode,
            exclusions = p[Keys.EXCLUSIONS] ?: defaults.exclusions,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setOnboardingDone(done: Boolean) = dataStore.edit { it[Keys.ONBOARDING] = done }
    suspend fun setLargeFileThresholdMb(value: Int) = dataStore.edit { it[Keys.LARGE_MB] = value.coerceIn(10, 10_000) }
    suspend fun setOldFileMonths(value: Int) = dataStore.edit { it[Keys.OLD_MONTHS] = value.coerceIn(1, 60) }
    suspend fun setTrashEnabled(value: Boolean) = dataStore.edit { it[Keys.TRASH] = value }
    suspend fun setTrashRetentionDays(value: Int) = dataStore.edit { it[Keys.RETENTION] = value.coerceIn(1, 90) }
    suspend fun setScheduleMode(value: ScheduleMode) = dataStore.edit { it[Keys.SCHEDULE] = value.name }
    suspend fun setNotifyThresholdMb(value: Int) = dataStore.edit { it[Keys.NOTIFY_MB] = value.coerceIn(50, 100_000) }
    suspend fun setShowHiddenFiles(value: Boolean) = dataStore.edit { it[Keys.HIDDEN] = value }
    suspend fun setAdvancedRootMode(value: Boolean) = dataStore.edit { it[Keys.ROOT] = value }
    suspend fun setThemeMode(value: ThemeMode) = dataStore.edit { it[Keys.THEME] = value.name }
    suspend fun addExclusion(path: String) = dataStore.edit { it[Keys.EXCLUSIONS] = (it[Keys.EXCLUSIONS] ?: emptySet()) + path.trimEnd('/') }
    suspend fun removeExclusion(path: String) = dataStore.edit { it[Keys.EXCLUSIONS] = (it[Keys.EXCLUSIONS] ?: emptySet()) - path }
}
