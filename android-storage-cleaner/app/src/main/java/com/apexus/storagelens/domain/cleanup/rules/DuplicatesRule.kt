package com.apexus.storagelens.domain.cleanup.rules

import com.apexus.storagelens.domain.cleanup.DuplicateFinder
import com.apexus.storagelens.domain.cleanup.PostScanRule
import com.apexus.storagelens.domain.cleanup.RuleContext
import com.apexus.storagelens.domain.cleanup.candidate
import com.apexus.storagelens.domain.model.CleanupCandidate
import com.apexus.storagelens.domain.model.CleanupCategory
import com.apexus.storagelens.domain.scan.ScannedFile

/**
 * Doublons : l'original conservé est le fichier le plus ancien du groupe ; les copies sont
 * marquées « recommandées » à l'intérieur de la catégorie (non cochée par défaut).
 */
class DuplicatesRule(
    private val sameSizeGroups: () -> List<List<ScannedFile>>,
    private val finder: DuplicateFinder = DuplicateFinder(),
) : PostScanRule {
    override val category = CleanupCategory.DUPLICATES

    override suspend fun find(ctx: RuleContext): List<CleanupCandidate> {
        val eligible = sameSizeGroups()
            .map { group -> group.filter { ctx.protectedPaths.canAutoPropose(it.path) } }
            .filter { it.size >= 2 }
        return finder.findDuplicates(eligible).flatMapIndexed { index, group ->
            val sorted = group.sortedWith(compareBy<ScannedFile> { it.lastModified }.thenBy { it.path.length })
            val original = sorted.first()
            sorted.drop(1).map { copy ->
                copy.candidate(category, recommended = true, detail = original.path, groupKey = "dup-$index")
            }
        }
    }
}
