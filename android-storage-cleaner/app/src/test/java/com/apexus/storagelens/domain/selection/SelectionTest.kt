package com.apexus.storagelens.domain.selection

import com.apexus.storagelens.domain.model.CleanupCandidate
import com.apexus.storagelens.domain.model.CleanupCategory
import com.apexus.storagelens.domain.model.CleanupGroup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SelectionTest {
    private fun c(path: String, cat: CleanupCategory, size: Long = 10, recommended: Boolean = true) =
        CleanupCandidate(path, path.substringAfterLast('/'), size, 0, false, cat, recommended)

    private val logs = CleanupGroup(CleanupCategory.LOGS, listOf(c("/r/a/1.log", CleanupCategory.LOGS), c("/r/a/2.log", CleanupCategory.LOGS), c("/r/b/3.log", CleanupCategory.LOGS, 100)))
    private val apks = CleanupGroup(CleanupCategory.APK_FILES, listOf(c("/r/x.apk", CleanupCategory.APK_FILES, recommended = false), c("/r/y.apk", CleanupCategory.APK_FILES)))
    private val media = CleanupGroup(CleanupCategory.MESSAGING_MEDIA, listOf(c("/r/wa", CleanupCategory.MESSAGING_MEDIA, 1000)))
    private val groups = listOf(logs, apks, media)

    @Test
    fun `tri state`() {
        assertEquals(TriState.OFF, Selection.stateOf(logs.candidates, emptySet()))
        assertEquals(TriState.INDETERMINATE, Selection.stateOf(logs.candidates, setOf("/r/a/1.log")))
        assertEquals(TriState.ON, Selection.stateOf(logs.candidates, logs.candidates.map { it.path }.toSet()))
    }

    @Test
    fun `toggle selects all from indeterminate then clears`() {
        val partial = setOf("/r/a/1.log", "/r/x.apk")
        val all = Selection.toggle(logs.candidates, partial)
        assertEquals(setOf("/r/a/1.log", "/r/a/2.log", "/r/b/3.log", "/r/x.apk"), all)
        assertEquals(setOf("/r/x.apk"), Selection.toggle(logs.candidates, all))
    }

    @Test
    fun `default and recommended selections exclude risky items`() {
        assertEquals(setOf("/r/a/1.log", "/r/a/2.log", "/r/b/3.log", "/r/y.apk"), Selection.defaultSelection(groups))
        assertEquals(Selection.defaultSelection(groups), Selection.recommended(groups))
        assertEquals(6, Selection.all(groups).size)
    }

    @Test
    fun `groups by parent sorted by weight and computes bytes`() {
        val parents = Selection.byParent(logs)
        assertEquals(listOf("/r/b", "/r/a"), parents.map { it.first })
        assertEquals(1110, Selection.selectedBytes(groups, setOf("/r/wa", "/r/a/1.log", "/r/b/3.log")))
    }

    @Test
    fun `system space is never negative`() {
        assertEquals(40, SystemSpace.compute(totalBytes = 100, freeBytes = 20, measuredBytes = 40))
        assertEquals(0, SystemSpace.compute(totalBytes = 100, freeBytes = 70, measuredBytes = 40))
    }
}
