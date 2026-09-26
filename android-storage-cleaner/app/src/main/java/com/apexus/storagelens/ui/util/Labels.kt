package com.apexus.storagelens.ui.util

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.ui.graphics.vector.ImageVector
import com.apexus.storagelens.R
import com.apexus.storagelens.domain.model.CleanupCategory
import com.apexus.storagelens.domain.model.FileType
import com.apexus.storagelens.domain.model.FileTypes
import com.apexus.storagelens.domain.model.RiskLevel
import com.apexus.storagelens.domain.model.StorageCategory

@StringRes
fun StorageCategory.labelRes(): Int = when (this) {
    StorageCategory.APPS -> R.string.category_apps
    StorageCategory.IMAGES -> R.string.category_images
    StorageCategory.VIDEOS -> R.string.category_videos
    StorageCategory.AUDIO -> R.string.category_audio
    StorageCategory.DOCUMENTS -> R.string.category_documents
    StorageCategory.ARCHIVES -> R.string.category_archives
    StorageCategory.APK -> R.string.category_apk
    StorageCategory.CACHE_TEMP -> R.string.category_cache_temp
    StorageCategory.LOGS_TRACES -> R.string.category_logs_traces
    StorageCategory.OTHER -> R.string.category_other
    StorageCategory.SYSTEM -> R.string.category_system
}

@StringRes
fun CleanupCategory.titleRes(): Int = when (this) {
    CleanupCategory.LOGS -> R.string.cleanup_logs
    CleanupCategory.TRACES -> R.string.cleanup_traces
    CleanupCategory.TEMP_FILES -> R.string.cleanup_temp
    CleanupCategory.CACHES -> R.string.cleanup_caches
    CleanupCategory.THUMBNAILS -> R.string.cleanup_thumbnails
    CleanupCategory.EMPTY_FOLDERS -> R.string.cleanup_empty_folders
    CleanupCategory.APK_FILES -> R.string.cleanup_apk
    CleanupCategory.ORPHAN_APP_DATA -> R.string.cleanup_orphans
    CleanupCategory.TRASH -> R.string.cleanup_trash
    CleanupCategory.DUPLICATES -> R.string.cleanup_duplicates
    CleanupCategory.MESSAGING_MEDIA -> R.string.cleanup_messaging
    CleanupCategory.LARGE_OLD_FILES -> R.string.cleanup_large_old
}

@StringRes
fun CleanupCategory.descriptionRes(): Int = when (this) {
    CleanupCategory.LOGS -> R.string.cleanup_logs_desc
    CleanupCategory.TRACES -> R.string.cleanup_traces_desc
    CleanupCategory.TEMP_FILES -> R.string.cleanup_temp_desc
    CleanupCategory.CACHES -> R.string.cleanup_caches_desc
    CleanupCategory.THUMBNAILS -> R.string.cleanup_thumbnails_desc
    CleanupCategory.EMPTY_FOLDERS -> R.string.cleanup_empty_folders_desc
    CleanupCategory.APK_FILES -> R.string.cleanup_apk_desc
    CleanupCategory.ORPHAN_APP_DATA -> R.string.cleanup_orphans_desc
    CleanupCategory.TRASH -> R.string.cleanup_trash_desc
    CleanupCategory.DUPLICATES -> R.string.cleanup_duplicates_desc
    CleanupCategory.MESSAGING_MEDIA -> R.string.cleanup_messaging_desc
    CleanupCategory.LARGE_OLD_FILES -> R.string.cleanup_large_old_desc
}

fun CleanupCategory.icon(): ImageVector = when (this) {
    CleanupCategory.LOGS -> Icons.Filled.ReceiptLong
    CleanupCategory.TRACES -> Icons.Filled.BugReport
    CleanupCategory.TEMP_FILES -> Icons.Filled.HourglassEmpty
    CleanupCategory.CACHES -> Icons.Filled.Cached
    CleanupCategory.THUMBNAILS -> Icons.Filled.PhotoLibrary
    CleanupCategory.EMPTY_FOLDERS -> Icons.Filled.FolderOff
    CleanupCategory.APK_FILES -> Icons.Filled.Android
    CleanupCategory.ORPHAN_APP_DATA -> Icons.Filled.AutoDelete
    CleanupCategory.TRASH -> Icons.Filled.Delete
    CleanupCategory.DUPLICATES -> Icons.Filled.ContentCopy
    CleanupCategory.MESSAGING_MEDIA -> Icons.Filled.Forum
    CleanupCategory.LARGE_OLD_FILES -> Icons.Filled.SdStorage
}

@StringRes
fun RiskLevel.labelRes(): Int = when (this) {
    RiskLevel.LOW -> R.string.risk_low
    RiskLevel.MEDIUM -> R.string.risk_medium
    RiskLevel.HIGH -> R.string.risk_high
}

@StringRes
fun FileType.labelRes(): Int = when (this) {
    FileType.IMAGE -> R.string.category_images
    FileType.VIDEO -> R.string.category_videos
    FileType.AUDIO -> R.string.category_audio
    FileType.DOCUMENT -> R.string.category_documents
    FileType.ARCHIVE -> R.string.category_archives
    FileType.APK -> R.string.category_apk
    FileType.OTHER -> R.string.category_other
}

fun iconForFile(name: String, isDirectory: Boolean): ImageVector = when {
    isDirectory -> Icons.Filled.Folder
    else -> when (FileTypes.of(name)) {
        FileType.IMAGE -> Icons.Filled.Image
        FileType.VIDEO -> Icons.Filled.Movie
        FileType.AUDIO -> Icons.Filled.MusicNote
        FileType.DOCUMENT -> Icons.Filled.Description
        FileType.ARCHIVE -> Icons.Filled.Archive
        FileType.APK -> Icons.Filled.Android
        FileType.OTHER -> Icons.Filled.InsertDriveFile
    }
}

fun StorageCategory.icon(): ImageVector = when (this) {
    StorageCategory.APPS -> Icons.Filled.Apps
    StorageCategory.IMAGES -> Icons.Filled.Image
    StorageCategory.VIDEOS -> Icons.Filled.Movie
    StorageCategory.AUDIO -> Icons.Filled.MusicNote
    StorageCategory.DOCUMENTS -> Icons.Filled.Description
    StorageCategory.ARCHIVES -> Icons.Filled.Archive
    StorageCategory.APK -> Icons.Filled.Android
    StorageCategory.CACHE_TEMP -> Icons.Filled.Cached
    StorageCategory.LOGS_TRACES -> Icons.Filled.ReceiptLong
    StorageCategory.OTHER -> Icons.Filled.InsertDriveFile
    StorageCategory.SYSTEM -> Icons.Filled.Memory
}
