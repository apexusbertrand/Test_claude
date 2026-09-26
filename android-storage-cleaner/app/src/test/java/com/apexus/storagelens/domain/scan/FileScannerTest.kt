package com.apexus.storagelens.domain.scan

import com.apexus.storagelens.domain.TestFs
import com.apexus.storagelens.domain.model.FileNode
import com.apexus.storagelens.domain.model.StorageCategory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import java.util.Collections

class FileScannerTest {
    private val fs = TestFs()

    @AfterEach
    fun tearDown() { fs.delete() }

    @Test
    fun `aggregates directory sizes recursively and sorts children by size`() = runTest {
        fs.file("a/one.bin", 100)
        fs.file("a/b/two.bin", 300)
        fs.file("c/three.bin", 50)
        fs.file("root.txt", 5)

        val root = FileScanner().scan(fs.root.path, ScanOptions(), object : ScanVisitor {})

        assertEquals(455, root.size)
        assertEquals(4, root.fileCount)
        val a = root.find(fs.path("a"))!!
        assertEquals(400, a.size)
        assertEquals(2, a.fileCount)
        assertEquals(listOf("a", "c", "root.txt"), root.children.map { it.name })
        assertEquals(300, root.find(fs.path("a/b"))!!.size)
    }

    @Test
    fun `does not follow symbolic links`() = runTest {
        fs.file("real/data.bin", 1000)
        Files.createSymbolicLink(fs.root.toPath().resolve("link"), fs.root.toPath().resolve("real"))
        Files.createSymbolicLink(fs.root.toPath().resolve("real/loop"), fs.root.toPath())

        val root = FileScanner().scan(fs.root.path, ScanOptions(), object : ScanVisitor {})

        assertEquals(1000, root.size)
        assertEquals(1, root.fileCount)
        assertNull(root.children.firstOrNull { it.name == "link" })
    }

    @Test
    fun `keeps only the largest files per directory and aggregates the rest`() = runTest {
        repeat(10) { i -> fs.file("many/f$i.bin", (i + 1) * 10) }

        val root = FileScanner().scan(fs.root.path, ScanOptions(maxFilesPerDirectory = 3), object : ScanVisitor {})
        val many = root.find(fs.path("many"))!!

        assertEquals(3, many.children.size)
        assertEquals(listOf(100L, 90L, 80L), many.children.map { it.size })
        assertEquals(7, many.hiddenFilesCount)
        assertEquals((1..7).sumOf { it * 10L }, many.hiddenFilesSize)
        assertEquals(550, many.size)
        assertEquals(10, many.fileCount)
    }

    @Test
    fun `honours excluded paths`() = runTest {
        fs.file("keep/a.bin", 10)
        fs.file("skip/b.bin", 1000)

        val root = FileScanner().scan(fs.root.path, ScanOptions(excludedPaths = setOf(fs.path("skip"))), object : ScanVisitor {})

        assertEquals(10, root.size)
        assertNull(root.find(fs.path("skip")))
    }

    @Test
    fun `visits every file and every directory in post-order`() = runTest {
        fs.file("x/y/z.bin", 1)
        fs.file("x/w.bin", 1)
        val files = Collections.synchronizedList(ArrayList<String>())
        val dirs = Collections.synchronizedList(ArrayList<FileNode>())

        FileScanner().scan(fs.root.path, ScanOptions(), object : ScanVisitor {
            override fun onFile(file: ScannedFile) { files += file.name }
            override fun onDirectory(node: FileNode) { dirs += node }
        })

        assertEquals(setOf("z.bin", "w.bin"), files.toSet())
        val order = dirs.map { it.name }
        assertTrue(order.indexOf("y") < order.indexOf("x"))
        assertEquals(fs.root.name, order.last())
    }

    @Test
    fun `counters track progress`() = runTest {
        fs.file("a/1.bin", 7)
        fs.file("b/2.bin", 3)
        val counters = ScanCounters()

        FileScanner().scan(fs.root.path, ScanOptions(), object : ScanVisitor {}, counters)

        assertEquals(2, counters.files.get())
        assertEquals(10, counters.bytes.get())
    }

    @Test
    fun `scan is cancellable`() = runTest {
        repeat(2000) { fs.file("d${it % 4}/f$it.bin", 1) }
        val cancelling = object : ScanVisitor {
            var seen = 0
            override fun onFile(file: ScannedFile) {
                if (++seen == 10) throw CancellationException("stop")
            }
        }
        assertThrows<CancellationException> {
            FileScanner().scan(fs.root.path, ScanOptions(parallelism = 1), cancelling)
        }
    }

    @Test
    fun `collectors classify categories and keep top files`() = runTest {
        fs.file("DCIM/photo.jpg", 500)
        fs.file("Movies/clip.mp4", 2000)
        fs.file("app/logs/trace.log", 30)
        fs.file("app/cache/blob", 40)
        fs.file("Download/setup.apk", 100)
        val categories = CategoryCollector(fs.root.path)
        val top = TopFilesCollector(limit = 2)

        FileScanner().scan(fs.root.path, ScanOptions(), CompositeVisitor(listOf(categories, top)))
        val byCategory = categories.result().associate { it.category to it.bytes }

        assertEquals(500L, byCategory[StorageCategory.IMAGES])
        assertEquals(2000L, byCategory[StorageCategory.VIDEOS])
        assertEquals(30L, byCategory[StorageCategory.LOGS_TRACES])
        assertEquals(40L, byCategory[StorageCategory.CACHE_TEMP])
        assertEquals(100L, byCategory[StorageCategory.APK])
        assertEquals(listOf("clip.mp4", "photo.jpg"), top.result().map { it.name })
        assertNotNull(byCategory)
    }
}
