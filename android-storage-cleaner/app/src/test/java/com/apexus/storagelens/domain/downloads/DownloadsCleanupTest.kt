package com.apexus.storagelens.domain.downloads

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DownloadsCleanupTest {
    private val now = 1_800_000_000_000L
    private val day = 24L * 3600 * 1000
    private fun f(name: String, ageDays: Long, size: Long = 10) = DownloadFile("/d/$name", name, size, now - ageDays * day)

    @Test
    fun `keeps only files older than the chosen number of days, largest first`() {
        val files = listOf(f("new.pdf", 2), f("old.apk", 45, 300), f("older.zip", 400, 900), f("limit.txt", 30))
        val result = DownloadsCleanup.olderThan(files, now, DownloadsCleanup.DEFAULT_DAYS)
        assertEquals(listOf("older.zip", "old.apk"), result.map { it.name })
        assertEquals(listOf("older.zip"), DownloadsCleanup.olderThan(files, now, 365).map { it.name })
    }

    @Test
    fun `ignores files without a modification date and clamps the number of days`() {
        val noDate = DownloadFile("/d/x", "x", 1, 0)
        assertEquals(emptyList<DownloadFile>(), DownloadsCleanup.olderThan(listOf(noDate), now, 30))
        assertEquals(1, DownloadsCleanup.clampDays(0))
        assertEquals(3650, DownloadsCleanup.clampDays(99_999))
        assertEquals(now - day, DownloadsCleanup.cutoff(now, -5))
    }
}
