package com.apexus.storagelens.domain.cleanup

import com.apexus.storagelens.domain.cleanup.rules.ApkFileRule
import com.apexus.storagelens.domain.cleanup.rules.CacheDirectoryRule
import com.apexus.storagelens.domain.cleanup.rules.EmptyFolderRule
import com.apexus.storagelens.domain.cleanup.rules.LargeOldFileRule
import com.apexus.storagelens.domain.cleanup.rules.LogFileRule
import com.apexus.storagelens.domain.cleanup.rules.MediaStoreTrashRule
import com.apexus.storagelens.domain.cleanup.rules.MessagingMediaRule
import com.apexus.storagelens.domain.cleanup.rules.OrphanAppDataRule
import com.apexus.storagelens.domain.cleanup.rules.TempFileRule
import com.apexus.storagelens.domain.cleanup.rules.ThumbnailsRule
import com.apexus.storagelens.domain.cleanup.rules.TraceFileRule
import com.apexus.storagelens.domain.cleanup.rules.TrashDirectoryRule
import com.apexus.storagelens.domain.model.CleanupCandidate
import com.apexus.storagelens.domain.model.CleanupCategory
import com.apexus.storagelens.domain.model.CleanupGroup
import com.apexus.storagelens.domain.model.FileNode
import com.apexus.storagelens.domain.scan.ScanVisitor
import com.apexus.storagelens.domain.scan.ScannedFile
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Applique les règles pendant le parcours (visiteur), puis consolide les résultats :
 * un élément dont un dossier parent est déjà proposé n'est pas compté deux fois.
 */
class CleanupAnalyzer(
    private val ctx: RuleContext,
    private val fileRules: List<FileRule> = defaultFileRules(),
    private val directoryRules: List<DirectoryRule> = defaultDirectoryRules(),
) : ScanVisitor {

    private val found = ConcurrentLinkedQueue<CleanupCandidate>()

    override fun onFile(file: ScannedFile) {
        if (!ctx.protectedPaths.canAutoPropose(file.path)) return
        // Première règle correspondante, dans l'ordre de priorité.
        fileRules.firstNotNullOfOrNull { it.match(file, ctx) }?.let(found::add)
    }

    override fun onDirectory(node: FileNode) {
        if (!ctx.protectedPaths.canAutoPropose(node.path)) return
        directoryRules.firstNotNullOfOrNull { it.match(node, ctx) }?.let(found::add)
    }

    /** Consolide les candidats du parcours et ceux des règles post-parcours. */
    suspend fun finish(postScanRules: List<PostScanRule> = emptyList()): List<CleanupGroup> {
        val all = ArrayList<CleanupCandidate>(found)
        postScanRules.forEach { all += it.find(ctx) }
        return group(deduplicate(all))
    }

    companion object {
        fun defaultFileRules(): List<FileRule> = listOf(
            MediaStoreTrashRule(),
            TraceFileRule(),
            LogFileRule(),
            TempFileRule(),
            ApkFileRule(),
            LargeOldFileRule(),
        )

        fun defaultDirectoryRules(): List<DirectoryRule> = listOf(
            ThumbnailsRule(),
            TrashDirectoryRule(),
            MessagingMediaRule(),
            OrphanAppDataRule(),
            CacheDirectoryRule(),
            EmptyFolderRule(),
        )

        /**
         * Retire les éléments dont un ancêtre est lui-même proposé, et les doublons de chemin
         * (le premier trouvé l'emporte).
         */
        fun deduplicate(candidates: List<CleanupCandidate>): List<CleanupCandidate> {
            val byPath = LinkedHashMap<String, CleanupCandidate>()
            candidates.forEach { byPath.putIfAbsent(it.path, it) }
            val directoryPaths = byPath.values.filter { it.isDirectory }.map { it.path }.toHashSet()
            return byPath.values.filter { candidate ->
                var parent = candidate.parentPath
                var covered = false
                while (parent.isNotEmpty()) {
                    if (parent in directoryPaths) { covered = true; break }
                    parent = parent.substringBeforeLast('/', "")
                }
                !covered
            }
        }

        fun group(candidates: List<CleanupCandidate>): List<CleanupGroup> =
            candidates.groupBy { it.category }
                .map { (category, items) -> CleanupGroup(category, items.sortedByDescending { it.size }) }
                .sortedWith(compareBy<CleanupGroup> { it.category.risk }.thenByDescending { it.totalBytes })

        /** Ordre d'affichage stable des catégories (utile pour l'UI). */
        val DISPLAY_ORDER: List<CleanupCategory> = CleanupCategory.entries
    }
}
