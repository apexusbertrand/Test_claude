package com.apexus.storagelens.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.apexus.storagelens.data.prefs.ScheduleMode
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager get() = WorkManager.getInstance(context)

    fun scheduleTrashPurge() {
        val request = PeriodicWorkRequestBuilder<TrashPurgeWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        workManager.enqueueUniquePeriodicWork(PURGE_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun applyScanSchedule(mode: ScheduleMode) {
        val days = when (mode) {
            ScheduleMode.OFF -> {
                workManager.cancelUniqueWork(SCAN_WORK)
                return
            }
            ScheduleMode.WEEKLY -> 7L
            ScheduleMode.MONTHLY -> 30L
        }
        val request = PeriodicWorkRequestBuilder<ScanWorker>(days, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiresDeviceIdle(true)
                    .build()
            )
            .build()
        workManager.enqueueUniquePeriodicWork(SCAN_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    companion object {
        private const val PURGE_WORK = "trash_purge"
        private const val SCAN_WORK = "scheduled_scan"
    }
}
