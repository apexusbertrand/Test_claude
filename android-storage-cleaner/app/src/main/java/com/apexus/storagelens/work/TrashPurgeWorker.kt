package com.apexus.storagelens.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.apexus.storagelens.data.delete.TrashRepository
import com.apexus.storagelens.data.prefs.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Vide quotidiennement les éléments de la corbeille interne ayant dépassé la durée de rétention. */
@HiltWorker
class TrashPurgeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val trash: TrashRepository,
    private val settings: SettingsRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        trash.purgeOlderThan(settings.current().trashRetentionDays)
        return Result.success()
    }
}
