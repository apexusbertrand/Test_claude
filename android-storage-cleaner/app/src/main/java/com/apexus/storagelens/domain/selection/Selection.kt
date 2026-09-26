package com.apexus.storagelens.domain.selection

import com.apexus.storagelens.domain.model.CleanupCandidate
import com.apexus.storagelens.domain.model.CleanupGroup
import com.apexus.storagelens.domain.model.RiskLevel

enum class TriState { ON, OFF, INDETERMINATE }

/** Logique de sélection à trois niveaux : catégorie → dossier parent → élément. */
object Selection {

    fun stateOf(items: Collection<CleanupCandidate>, selected: Set<String>): TriState {
        if (items.isEmpty()) return TriState.OFF
        val count = items.count { it.path in selected }
        return when (count) {
            0 -> TriState.OFF
            items.size -> TriState.ON
            else -> TriState.INDETERMINATE
        }
    }

    /** Coche tout si l'état n'est pas ON, sinon décoche tout. */
    fun toggle(items: Collection<CleanupCandidate>, selected: Set<String>): Set<String> {
        val paths = items.map { it.path }
        return if (stateOf(items, selected) == TriState.ON) selected - paths.toSet() else selected + paths
    }

    fun toggleOne(path: String, selected: Set<String>): Set<String> =
        if (path in selected) selected - path else selected + path

    /** Regroupe les éléments d'une catégorie par dossier parent, du plus lourd au plus léger. */
    fun byParent(group: CleanupGroup): List<Pair<String, List<CleanupCandidate>>> =
        group.candidates.groupBy { it.parentPath }
            .map { it.key to it.value }
            .sortedByDescending { (_, items) -> items.sumOf { it.size } }

    /** Sélection initiale : catégories cochées par défaut, éléments recommandés uniquement. */
    fun defaultSelection(groups: List<CleanupGroup>): Set<String> =
        groups.filter { it.category.selectedByDefault }
            .flatMap { g -> g.candidates.filter { it.recommended } }
            .map { it.path }
            .toSet()

    /** « Sélection recommandée » : éléments recommandés de risque faible. */
    fun recommended(groups: List<CleanupGroup>): Set<String> =
        groups.filter { it.category.risk == RiskLevel.LOW }
            .flatMap { g -> g.candidates.filter { it.recommended } }
            .map { it.path }
            .toSet()

    fun all(groups: List<CleanupGroup>): Set<String> =
        groups.flatMap { g -> g.candidates.map { it.path } }.toSet()

    fun selectedBytes(groups: List<CleanupGroup>, selected: Set<String>): Long =
        groups.sumOf { g -> g.candidates.filter { it.path in selected }.sumOf { it.size } }

    fun selectedItems(groups: List<CleanupGroup>, selected: Set<String>): List<CleanupCandidate> =
        groups.flatMap { g -> g.candidates.filter { it.path in selected } }
}

/** Espace « Système & autres » : ce qui n'est attribuable à aucune catégorie mesurée. */
object SystemSpace {
    fun compute(totalBytes: Long, freeBytes: Long, measuredBytes: Long): Long =
        (totalBytes - freeBytes - measuredBytes).coerceAtLeast(0)
}
