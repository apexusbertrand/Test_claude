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
import com.apexus.storagelens.domain.model.FileNode
import com.apexus.storagelens.domain.scan.ScannedFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RulesTest {
    private val root = "/storage/emulated/0"
    private val now = 1_800_000_000_000L
    private val day = 24L * 3600 * 1000
    private val ctx = RuleContext(
        storageRoot = root,
        protectedPaths = ProtectedPaths(listOf(root), "com.apexus.storagelens"),
        installedPackages = setOf("com.whatsapp", "org.telegram.messenger"),
        nowMillis = now,
        largeFileThresholdBytes = 100L * 1024 * 1024,
        oldFileAgeMillis = 180 * day,
        apkPackageResolver = { if (it.endsWith("whatsapp.apk")) "com.whatsapp" else null },
    )

    private fun file(rel: String, size: Long = 10, ageDays: Long = 10): ScannedFile {
        val path = "$root/$rel"
        return ScannedFile(path, path.substringAfterLast('/'), path.substringBeforeLast('/'), size, now - ageDays * day)
    }

    private fun dir(rel: String, size: Long = 100, files: Int = 1) =
        FileNode("$root/$rel", rel.substringAfterLast('/'), true, size, files, now)

    @Test
    fun `log rule`() {
        val rule = LogFileRule()
        assertNotNull(rule.match(file("app/debug.log"), ctx))
        assertNotNull(rule.match(file("app/debug.log.3"), ctx))
        assertNotNull(rule.match(file("app/logs/session.txt"), ctx))
        assertNull(rule.match(file("Documents/catalog.txt"), ctx))
    }

    @Test
    fun `trace rule`() {
        val rule = TraceFileRule()
        assertNotNull(rule.match(file("x/heap.hprof"), ctx))
        assertNotNull(rule.match(file("x/crashes/abc"), ctx))
        assertNotNull(rule.match(file("bugreport-2025-01-01.zip"), ctx))
        assertNull(rule.match(file("Music/track.mp3"), ctx))
    }

    @Test
    fun `temp rule skips files being written and does not recommend backups`() {
        val rule = TempFileRule()
        assertTrue(rule.match(file("Download/video.crdownload"), ctx)!!.recommended)
        val recent = ScannedFile("$root/a.tmp", "a.tmp", root, 1, now - 1000)
        assertNull(rule.match(recent, ctx))
        assertFalse(rule.match(file("notes.bak"), ctx)!!.recommended)
    }

    @Test
    fun `apk rule recommends only apks already installed`() {
        val rule = ApkFileRule()
        assertTrue(rule.match(file("Download/whatsapp.apk"), ctx)!!.recommended)
        assertFalse(rule.match(file("Download/game.apk"), ctx)!!.recommended)
        assertNull(rule.match(file("Download/game.zip"), ctx))
    }

    @Test
    fun `mediastore trashed files`() {
        assertNotNull(MediaStoreTrashRule().match(file("DCIM/.trashed-1700000000-IMG.jpg"), ctx))
        assertNull(MediaStoreTrashRule().match(file("DCIM/IMG.jpg"), ctx))
    }

    @Test
    fun `large old files`() {
        val rule = LargeOldFileRule()
        assertNotNull(rule.match(file("Movies/old.mkv", size = 200L * 1024 * 1024, ageDays = 400), ctx))
        assertNull(rule.match(file("Movies/new.mkv", size = 200L * 1024 * 1024, ageDays = 10), ctx))
        assertNull(rule.match(file("Movies/small.mkv", size = 1024, ageDays = 400), ctx))
    }

    @Test
    fun `directory rules`() {
        assertNotNull(CacheDirectoryRule().match(dir("app/cache"), ctx))
        assertNull(CacheDirectoryRule().match(dir("app/cache", size = 0, files = 0), ctx))
        assertNotNull(ThumbnailsRule().match(dir("DCIM/.thumbnails"), ctx))
        assertNotNull(TrashDirectoryRule().match(dir(".Trash-1000"), ctx))
        assertNotNull(EmptyFolderRule().match(dir("old/empty", size = 0, files = 0), ctx))
        assertNull(EmptyFolderRule().match(dir("old/full"), ctx))
    }

    @Test
    fun `orphan app data only for uninstalled packages at known locations`() {
        val rule = OrphanAppDataRule()
        assertNotNull(rule.match(dir("Android/data/com.removed.game"), ctx))
        assertNotNull(rule.match(dir("com.removed.game"), ctx))
        assertNull(rule.match(dir("Android/data/com.whatsapp"), ctx))
        assertNull(rule.match(dir("Documents/com.removed.game"), ctx))
        assertNull(rule.match(dir("my.photos"), ctx))
    }

    @Test
    fun `messaging media subfolders`() {
        val rule = MessagingMediaRule()
        assertEquals("WhatsApp", rule.match(dir("Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video"), ctx)!!.detail)
        assertEquals("WhatsApp", rule.match(dir("WhatsApp/Media/.Statuses"), ctx)!!.detail)
        assertEquals("Telegram", rule.match(dir("Telegram/Telegram Video"), ctx)!!.detail)
        assertNull(rule.match(dir("WhatsApp/Databases"), ctx))
    }
}
