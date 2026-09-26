package com.apexus.storagelens.domain.scan

import com.apexus.storagelens.domain.model.FileNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TreeOpsTest {
    private fun f(path: String, size: Long) = FileNode(path, path.substringAfterLast('/'), false, size, 1, 0)

    private val tree = FileNode(
        "/r", "r", true, size = 1110, fileCount = 5, lastModified = 0,
        children = listOf(
            FileNode(
                "/r/a", "a", true, size = 1100, fileCount = 4, lastModified = 0,
                children = listOf(
                    FileNode("/r/a/b", "b", true, 1000, 2, 0, children = listOf(f("/r/a/b/big", 900), f("/r/a/b/small", 100))),
                    f("/r/a/x", 60),
                ),
                hiddenFilesCount = 1, hiddenFilesSize = 40,
            ),
            f("/r/z", 10),
        ),
    )

    @Test
    fun `removes files and directories and adjusts ancestors`() {
        val result = TreeOps.remove(
            tree,
            listOf(RemovedEntry("/r/a/b", 1000, 2), RemovedEntry("/r/a/b/big", 900, 1), RemovedEntry("/r/z", 10, 1)),
        )
        assertEquals(100, result.size)
        assertEquals(2, result.fileCount)
        assertNull(result.find("/r/a/b"))
        assertNull(result.find("/r/z"))
        assertEquals(100, result.find("/r/a")!!.size)
    }

    @Test
    fun `removing a hidden file adjusts hidden counters`() {
        val result = TreeOps.remove(tree, listOf(RemovedEntry("/r/a/hidden.bin", 40, 1)))
        val a = result.find("/r/a")!!
        assertEquals(0, a.hiddenFilesCount)
        assertEquals(0, a.hiddenFilesSize)
        assertEquals(1060, a.size)
        assertEquals(1070, result.size)
    }

    @Test
    fun `normalize drops entries covered by an ancestor`() {
        val n = TreeOps.normalize(listOf(RemovedEntry("/r/a/b/c", 1, 1), RemovedEntry("/r/a", 5, 3), RemovedEntry("/r/ab", 1, 1)))
        assertEquals(listOf("/r/a", "/r/ab"), n.map { it.path })
    }
}
