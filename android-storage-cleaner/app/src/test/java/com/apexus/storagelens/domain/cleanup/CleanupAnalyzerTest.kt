package com.apexus.storagelens.domain.cleanup

import com.apexus.storagelens.domain.TestFs
import com.apexus.storagelens.domain.cleanup.rules.DuplicatesRule
import com.apexus.storagelens.domain.model.CleanupCandidate
import com.apexus.storagelens.domain.model.CleanupCategory
import com.apexus.storagelens.domain.scan.CompositeVisitor
import com.apexus.storagelens.domain.scan.FileScanner
import com.apexus.storagelens.domain.scan.ScanOptions
import com.apexus.storagelens.domain.scan.SizeIndexCollector
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class CleanupAnalyzerTest {
    private val fs = TestFs()
    private val day = 24L * 3600 * 1000

    @AfterEach
    fun tearDown() { fs.delete() }

    private fun ctx() = RuleContext(
        storageRoot = fs.root.path,
        protectedPaths = ProtectedPaths(listOf(fs.root.path), "com.apexus.storagelens"),
        installedPackages = emptySet(),
        nowMillis = System.currentTimeMillis(),
        largeFileThresholdBytes = Long.MAX_VALUE,
        oldFileAgeMillis = 365 * day,
    )

    @Test
    fun `end to end analysis groups candidates without double counting`() = runTest {
        fs.file("app/cache/a.log", 100) // couvert par le dossier cache
        fs.file("app/cache/b.bin", 200)
        fs.file("app/debug.log", 50)
        fs.file("Download/file.tmp", 20, ageMillis = 2 * day)
        fs.file("DCIM/.thumbnails/t1.jpg", 30)
        fs.file("DCIM/Camera/IMG_1.jpg", 1000)
        fs.file("DCIM/Camera/crash.log", 5) // dossier Camera : jamais proposé automatiquement
        fs.dir("empty/nested/deeper")

        val analyzer = CleanupAnalyzer(ctx())
        FileScanner().scan(fs.root.path, ScanOptions(), analyzer)
        val groups = analyzer.finish().associateBy { it.category }

        val caches = groups.getValue(CleanupCategory.CACHES)
        assertEquals(listOf(fs.path("app/cache")), caches.candidates.map { it.path })
        assertEquals(300, caches.totalBytes)
        assertEquals(listOf(fs.path("app/debug.log")), groups.getValue(CleanupCategory.LOGS).candidates.map { it.path })
        assertEquals(1, groups.getValue(CleanupCategory.TEMP_FILES).count)
        assertEquals(1, groups.getValue(CleanupCategory.THUMBNAILS).count)
        assertEquals(listOf(fs.path("empty")), groups.getValue(CleanupCategory.EMPTY_FOLDERS).candidates.map { it.path })
        val all = groups.values.flatMap { it.candidates }.map { it.path }
        assertFalse(all.any { it.contains("/DCIM/Camera") })
    }

    @Test
    fun `deduplicate keeps ancestors and first occurrence`() {
        fun c(path: String, dir: Boolean, cat: CleanupCategory = CleanupCategory.CACHES) =
            CleanupCandidate(path, path.substringAfterLast('/'), 1, 0, dir, cat, true)
        val result = CleanupAnalyzer.deduplicate(
            listOf(
                c("/r/a/b/file.log", false, CleanupCategory.LOGS),
                c("/r/a", true),
                c("/r/a b/file", false),
                c("/r/x.tmp", false, CleanupCategory.TEMP_FILES),
                c("/r/x.tmp", false, CleanupCategory.LARGE_OLD_FILES),
            )
        )
        assertEquals(listOf("/r/a", "/r/a b/file", "/r/x.tmp"), result.map { it.path })
        assertEquals(CleanupCategory.TEMP_FILES, result.last().category)
    }

    @Test
    fun `duplicates keep the oldest file as original`() = runTest {
        val content = Random(42).nextBytes(300 * 1024)
        fs.file("a/original.bin", content = content, ageMillis = 10 * day)
        fs.file("b/copy1.bin", content = content, ageMillis = 2 * day)
        fs.file("c/copy2.bin", content = content, ageMillis = 1 * day)
        val different = content.copyOf().also { it[150_000] = (it[150_000] + 1).toByte() }
        fs.file("d/sameSizeDifferent.bin", content = different)

        val sizes = SizeIndexCollector(minSize = 1024)
        val analyzer = CleanupAnalyzer(ctx())
        FileScanner().scan(fs.root.path, ScanOptions(), CompositeVisitor(listOf(sizes, analyzer)))
        val groups = analyzer.finish(listOf(DuplicatesRule(sizes::sameSizeGroups)))
        val dups = groups.first { it.category == CleanupCategory.DUPLICATES }

        assertEquals(setOf(fs.path("b/copy1.bin"), fs.path("c/copy2.bin")), dups.candidates.map { it.path }.toSet())
        assertTrue(dups.candidates.all { it.detail == fs.path("a/original.bin") })
        assertNull(dups.candidates.firstOrNull { it.path.contains("sameSizeDifferent") })
    }

    @Test
    fun `duplicate finder handles small files with partial hash only`() = runTest {
        fs.file("x/1.txt", content = "hello".toByteArray())
        fs.file("y/2.txt", content = "hello".toByteArray())
        fs.file("z/3.txt", content = "hellp".toByteArray())
        val sizes = SizeIndexCollector(minSize = 1)
        FileScanner().scan(fs.root.path, ScanOptions(), sizes)

        val dups = DuplicateFinder().findDuplicates(sizes.sameSizeGroups())

        assertEquals(1, dups.size)
        assertEquals(setOf("1.txt", "2.txt"), dups.single().map { it.name }.toSet())
    }
}
