package com.apexus.storagelens.data.apps

import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import com.apexus.storagelens.data.storage.PermissionChecker
import com.apexus.storagelens.domain.model.AppStorageInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppStorageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissions: PermissionChecker,
) {
    private val packageManager: PackageManager = context.packageManager
    private val statsManager = context.getSystemService(StorageStatsManager::class.java)
    private val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)

    fun installedPackages(): Set<String> = installedApplications().map { it.packageName }.toSet()

    /** Nom de paquet contenu dans un fichier APK (null si illisible). */
    fun apkPackageName(path: String): String? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageArchiveInfo(path, PackageManager.PackageInfoFlags.of(0))?.packageName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageArchiveInfo(path, 0)?.packageName
        }
    } catch (e: Exception) {
        null
    }

    /**
     * Taille de chaque application (APK, données, cache). Nécessite l'accès aux données
     * d'utilisation : sans lui, seule l'application elle-même peut être mesurée.
     */
    suspend fun appsWithStorage(): List<AppStorageInfo> = withContext(Dispatchers.IO) {
        val lastUsed = lastUsedByPackage()
        val canQueryOthers = permissions.hasUsageAccess()
        val user = Process.myUserHandle()
        installedApplications().mapNotNull { info ->
            if (!canQueryOthers && info.packageName != context.packageName) return@mapNotNull null
            val stats = try {
                statsManager.queryStatsForPackage(info.storageUuid, info.packageName, user)
            } catch (e: PackageManager.NameNotFoundException) {
                return@mapNotNull null
            } catch (e: SecurityException) {
                return@mapNotNull null
            } catch (e: IOException) {
                return@mapNotNull null
            }
            AppStorageInfo(
                packageName = info.packageName,
                label = info.loadLabel(packageManager).toString(),
                versionName = versionName(info.packageName),
                apkBytes = stats.appBytes,
                dataBytes = stats.dataBytes,
                cacheBytes = stats.cacheBytes,
                lastUsedMillis = lastUsed[info.packageName],
                isSystemApp = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                    (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0,
            )
        }.sortedByDescending { it.totalBytes }
    }

    fun appIcon(packageName: String) = try {
        packageManager.getApplicationIcon(packageName)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    private fun installedApplications(): List<ApplicationInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledApplications(0)
        }

    private fun versionName(packageName: String): String? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).versionName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionName
        }
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    private fun lastUsedByPackage(): Map<String, Long> {
        if (!permissions.hasUsageAccess()) return emptyMap()
        val end = System.currentTimeMillis()
        val start = end - TimeUnit.DAYS.toMillis(365)
        return try {
            usageStatsManager.queryAndAggregateUsageStats(start, end)
                .mapValues { it.value.lastTimeUsed }
                .filterValues { it > 0 }
        } catch (e: SecurityException) {
            emptyMap()
        }
    }
}
