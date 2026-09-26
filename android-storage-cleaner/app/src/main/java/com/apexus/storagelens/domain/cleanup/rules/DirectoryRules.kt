package com.apexus.storagelens.domain.cleanup.rules

import com.apexus.storagelens.domain.cleanup.DirectoryRule
import com.apexus.storagelens.domain.cleanup.PathPatterns
import com.apexus.storagelens.domain.cleanup.RuleContext
import com.apexus.storagelens.domain.cleanup.candidate
import com.apexus.storagelens.domain.model.CleanupCandidate
import com.apexus.storagelens.domain.model.CleanupCategory
import com.apexus.storagelens.domain.model.FileNode

private fun FileNode.relativeTo(root: String): String = path.removePrefix(root.trimEnd('/')).trimStart('/')

class CacheDirectoryRule : DirectoryRule {
    override val category = CleanupCategory.CACHES
    override fun match(dir: FileNode, ctx: RuleContext): CleanupCandidate? =
        if (dir.size > 0 && PathPatterns.isCacheDirectoryName(dir.name)) dir.candidate(category) else null
}

class ThumbnailsRule : DirectoryRule {
    override val category = CleanupCategory.THUMBNAILS
    override fun match(dir: FileNode, ctx: RuleContext): CleanupCandidate? =
        if (dir.size > 0 && dir.name.equals(".thumbnails", ignoreCase = true)) dir.candidate(category) else null
}

class EmptyFolderRule : DirectoryRule {
    override val category = CleanupCategory.EMPTY_FOLDERS
    override fun match(dir: FileNode, ctx: RuleContext): CleanupCandidate? {
        if (dir.fileCount != 0 || dir.size != 0L || dir.hiddenFilesCount != 0) return null
        if (dir.relativeTo(ctx.storageRoot).isEmpty()) return null
        return dir.candidate(category)
    }
}

class TrashDirectoryRule : DirectoryRule {
    override val category = CleanupCategory.TRASH
    override fun match(dir: FileNode, ctx: RuleContext): CleanupCandidate? =
        if (dir.size > 0 && PathPatterns.isTrashDirectoryName(dir.name)) dir.candidate(category) else null
}

/** Dossiers nommés d'après un paquet dont l'application n'est plus installée. */
class OrphanAppDataRule : DirectoryRule {
    override val category = CleanupCategory.ORPHAN_APP_DATA
    override fun match(dir: FileNode, ctx: RuleContext): CleanupCandidate? {
        if (ctx.installedPackages.isEmpty()) return null // liste inconnue : on s'abstient
        if (!PathPatterns.looksLikePackageName(dir.name)) return null
        if (dir.name in ctx.installedPackages) return null
        val parent = dir.relativeTo(ctx.storageRoot).substringBeforeLast('/', "").lowercase()
        val allowedParent = parent.isEmpty() || parent in ANDROID_PACKAGE_PARENTS
        return if (allowedParent && dir.size > 0) dir.candidate(category, detail = dir.name) else null
    }

    companion object {
        val ANDROID_PACKAGE_PARENTS = setOf("android/data", "android/obb", "android/media")
    }
}

/** Médias reçus via les messageries (WhatsApp, Telegram…), proposés par sous-dossier. */
class MessagingMediaRule : DirectoryRule {
    override val category = CleanupCategory.MESSAGING_MEDIA
    override fun match(dir: FileNode, ctx: RuleContext): CleanupCandidate? {
        if (dir.size <= 0) return null
        val rel = dir.relativeTo(ctx.storageRoot)
        val parent = rel.substringBeforeLast('/', "")
        val app = MEDIA_PARENTS.entries.firstOrNull { (suffix, _) -> parent.equals(suffix, ignoreCase = true) || parent.endsWith("/$suffix", ignoreCase = true) }
            ?.value ?: return null
        return dir.candidate(category, detail = app)
    }

    companion object {
        private val MEDIA_PARENTS = linkedMapOf(
            "WhatsApp/Media" to "WhatsApp",
            "WhatsApp Business/Media" to "WhatsApp Business",
            "Telegram" to "Telegram",
            "Signal" to "Signal",
            "Viber/media" to "Viber",
            "Messenger" to "Messenger",
        )
    }
}
