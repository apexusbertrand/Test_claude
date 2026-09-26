package com.apexus.storagelens.domain.apps

import com.apexus.storagelens.domain.model.AppStorageInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppBatchTest {
    private fun app(pkg: String, cache: Long, total: Long = 100, system: Boolean = false) =
        AppStorageInfo(pkg, pkg.uppercase(), null, apkBytes = total, dataBytes = cache, cacheBytes = cache, lastUsedMillis = null, isSystemApp = system)

    private val apps = listOf(
        app("a.small", cache = 10),
        app("b.big", cache = 500, total = 900),
        app("c.system", cache = 300, system = true),
        app("d.nocache", cache = 0),
        app("me.own", cache = 50),
    )

    @Test
    fun `clear cache targets apps with cache, largest first`() {
        val batch = AppBatch.create(AppBatchKind.CLEAR_CACHE, apps, ownPackage = "me.own")
        assertEquals(listOf("b.big", "c.system", "me.own", "a.small"), batch.packages)
        assertEquals(1, batch.skipped)
    }

    @Test
    fun `uninstall skips system apps and the app itself`() {
        val batch = AppBatch.create(AppBatchKind.UNINSTALL, apps, ownPackage = "me.own")
        assertEquals(setOf("a.small", "b.big", "d.nocache"), batch.packages.toSet())
        assertEquals("b.big", batch.packages.first())
        assertEquals(2, batch.skipped)
    }

    @Test
    fun `steps through the queue`() {
        var batch = AppBatch.create(AppBatchKind.UNINSTALL, apps.take(2), ownPackage = "me.own")
        assertEquals("b.big", batch.current)
        assertEquals("B.BIG", batch.currentLabel)
        batch = batch.markLaunched()
        assertTrue(batch.launched)
        batch = batch.next()
        assertFalse(batch.launched)
        assertEquals("a.small", batch.current)
        batch = batch.next()
        assertTrue(batch.isFinished)
        assertNull(batch.current)
    }
}
