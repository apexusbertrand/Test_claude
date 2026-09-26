package com.apexus.storagelens.data.storage

import android.content.Context
import com.apexus.storagelens.data.apps.AppStorageRepository
import com.apexus.storagelens.data.prefs.SettingsRepository
import com.apexus.storagelens.domain.cleanup.ProtectedPaths
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Construit le garde-fou [ProtectedPaths] à partir de l'état courant de l'appareil. */
@Singleton
class ProtectionProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val volumes: StorageVolumesRepository,
    private val apps: AppStorageRepository,
    private val settings: SettingsRepository,
) {
    suspend fun current(): ProtectedPaths = withContext(Dispatchers.IO) {
        ProtectedPaths(
            storageRoots = volumes.storageRoots(),
            ownPackage = context.packageName,
            userExclusions = settings.current().exclusions,
            installedPackages = apps.installedPackages().ifEmpty { null },
        )
    }
}
