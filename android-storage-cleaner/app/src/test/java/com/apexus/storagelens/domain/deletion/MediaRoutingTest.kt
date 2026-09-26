package com.apexus.storagelens.domain.deletion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MediaRoutingTest {
    private val indexed = MediaIndexEntry(isTrashed = false)

    private fun route(
        name: String = "IMG_1.jpg",
        isDirectory: Boolean = false,
        allowTrash: Boolean = true,
        trashEnabled: Boolean = true,
        supported: Boolean = true,
        index: MediaIndexEntry? = indexed,
    ) = MediaRouting.route(name, isDirectory, allowTrash, trashEnabled, supported, index)

    @Test
    fun `photos and videos go through the system confirmation`() {
        assertEquals(DeletionRoute.SYSTEM_TRASH, route("IMG_1.jpg"))
        assertEquals(DeletionRoute.SYSTEM_TRASH, route("VID_1.mp4"))
        assertEquals(DeletionRoute.SYSTEM_DELETE, route(trashEnabled = false))
        assertEquals(DeletionRoute.SYSTEM_DELETE, route(allowTrash = false))
    }

    @Test
    fun `items already in the android trash are deleted permanently`() {
        assertEquals(DeletionRoute.SYSTEM_DELETE, route(".trashed-1700000000-IMG_1.jpg", index = MediaIndexEntry(isTrashed = true)))
    }

    @Test
    fun `other items are handled directly`() {
        assertEquals(DeletionRoute.DIRECT, route("song.mp3"))
        assertEquals(DeletionRoute.DIRECT, route("report.pdf"))
        assertEquals(DeletionRoute.DIRECT, route("WhatsApp Video", isDirectory = true))
        assertEquals(DeletionRoute.DIRECT, route(index = null))
        assertEquals(DeletionRoute.DIRECT, route(supported = false))
    }
}
