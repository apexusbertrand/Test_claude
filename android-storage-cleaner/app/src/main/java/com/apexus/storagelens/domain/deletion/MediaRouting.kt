package com.apexus.storagelens.domain.deletion

import com.apexus.storagelens.domain.model.FileType
import com.apexus.storagelens.domain.model.FileTypes

/** Comment un élément est supprimé. */
enum class DeletionRoute {
    /** Photo/vidéo envoyée dans la corbeille Android après la confirmation système. */
    SYSTEM_TRASH,

    /** Photo/vidéo supprimée définitivement après la confirmation système. */
    SYSTEM_DELETE,

    /** Traité directement par l'application (corbeille interne ou suppression). */
    DIRECT,
}

/** État d'un fichier dans l'index MediaStore. */
data class MediaIndexEntry(val isTrashed: Boolean)

object MediaRouting {

    fun isPhotoOrVideo(name: String): Boolean = FileTypes.of(name).let { it == FileType.IMAGE || it == FileType.VIDEO }

    /**
     * Toute photo ou vidéo indexée passe par la fenêtre de confirmation d'Android quand le
     * système la propose (Android 11+). Les dossiers et les fichiers non indexés restent
     * traités directement, après la confirmation de l'application.
     */
    fun route(
        name: String,
        isDirectory: Boolean,
        allowTrash: Boolean,
        trashEnabled: Boolean,
        systemConfirmationSupported: Boolean,
        index: MediaIndexEntry?,
    ): DeletionRoute = when {
        isDirectory || !systemConfirmationSupported || index == null || !isPhotoOrVideo(name) -> DeletionRoute.DIRECT
        // Déjà dans la corbeille Android : la seule action possible est la suppression définitive.
        index.isTrashed -> DeletionRoute.SYSTEM_DELETE
        trashEnabled && allowTrash -> DeletionRoute.SYSTEM_TRASH
        else -> DeletionRoute.SYSTEM_DELETE
    }
}
