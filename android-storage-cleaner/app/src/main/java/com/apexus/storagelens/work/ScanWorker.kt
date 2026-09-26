package com.apexus.storagelens.work

import android.content.Context
import android.text.format.Formatter
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.apexus.storagelens.data.prefs.SettingsRepository
import com.apexus.storagelens.data.scan.ScanManager
import com.apexus.storagelens.data.storage.PermissionChecker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Analyse planifiée : elle ne supprime JAMAIS rien, elle se contente de notifier
 * l'espace récupérable lorsqu'il dépasse le seuil choisi.
 */
@HiltWorker
class ScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val scanManager: ScanManager,
    private val settings: SettingsRepository,
    private val permissions: PermissionChecker,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!permissions.hasAllFilesAccess() || scanManager.isRunning) return Result.success()
        return try {
            val result = scanManager.scan(volumeId = null)
            val threshold = settings.current().notifyThresholdBytes
            if (result.recommendedBytes >= threshold) {
                Notifications.showRecoverable(
                    applicationContext,
                    Formatter.formatShortFileSize(applicationContext, result.recommendedBytes),
                )
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
