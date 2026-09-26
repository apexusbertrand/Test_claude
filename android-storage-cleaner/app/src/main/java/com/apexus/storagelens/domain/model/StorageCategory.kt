package com.apexus.storagelens.domain.model

/** Catégories affichées sur le tableau de bord. */
enum class StorageCategory {
    APPS, IMAGES, VIDEOS, AUDIO, DOCUMENTS, ARCHIVES, APK, CACHE_TEMP, LOGS_TRACES, OTHER,

    /** Corbeille interne de l'application (exclue de l'analyse, mesurée à part). */
    APP_TRASH,
    SYSTEM;

    companion object {
        fun fromType(type: FileType): StorageCategory = when (type) {
            FileType.IMAGE -> IMAGES
            FileType.VIDEO -> VIDEOS
            FileType.AUDIO -> AUDIO
            FileType.DOCUMENT -> DOCUMENTS
            FileType.ARCHIVE -> ARCHIVES
            FileType.APK -> APK
            FileType.OTHER -> OTHER
        }
    }
}

data class CategoryUsage(val category: StorageCategory, val bytes: Long)
