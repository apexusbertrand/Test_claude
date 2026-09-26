package com.apexus.storagelens.domain.cleanup

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProtectedPathsTest {
    private val root = "/storage/emulated/0"
    private val paths = ProtectedPaths(
        storageRoots = listOf(root, "/storage/1234-ABCD"),
        ownPackage = "com.apexus.storagelens",
        userExclusions = setOf("$root/Keep"),
        installedPackages = setOf("com.whatsapp"),
    )

    @Test
    fun `roots and standard directories are protected`() {
        assertTrue(paths.isProtected(root))
        assertTrue(paths.isProtected("$root/"))
        assertTrue(paths.isProtected("/storage/1234-ABCD"))
        assertTrue(paths.isProtected("$root/DCIM"))
        assertTrue(paths.isProtected("$root/Download"))
        assertTrue(paths.isProtected("$root/Documents"))
        assertTrue(paths.isProtected("$root/DCIM/Camera"))
        assertTrue(paths.isProtected("$root/Android"))
        assertTrue(paths.isProtected("$root/Android/data"))
    }

    @Test
    fun `paths outside storage volumes are protected`() {
        assertTrue(paths.isProtected("/data/log"))
        assertTrue(paths.isProtected("/system/app"))
        assertTrue(paths.isProtected("/storage/emulated/01/x"))
    }

    @Test
    fun `package folders are protected unless the app is uninstalled`() {
        assertTrue(paths.isProtected("$root/Android/data/com.whatsapp"))
        assertTrue(paths.isProtected("$root/Android/data/com.apexus.storagelens"))
        assertFalse(paths.isProtected("$root/Android/data/com.removed.app"))
        assertFalse(paths.isProtected("$root/Android/data/com.whatsapp/cache"))
        assertTrue(paths.isProtected("$root/Android/data/com.apexus.storagelens/files/trash"))
        assertFalse(paths.isProtected("$root/Android/data/com.apexus.storagelens/cache"))
    }

    @Test
    fun `unknown installed packages keep every package folder protected`() {
        val unknown = ProtectedPaths(listOf(root), "com.apexus.storagelens")
        assertTrue(unknown.isProtected("$root/Android/data/com.removed.app"))
    }

    @Test
    fun `camera files can be deleted manually but never proposed automatically`() {
        val photo = "$root/DCIM/Camera/IMG_1.jpg"
        assertTrue(paths.canDelete(photo))
        assertFalse(paths.canAutoPropose(photo))
        assertTrue(paths.canAutoPropose("$root/DCIM/.thumbnails"))
    }

    @Test
    fun `user exclusions are never deletable`() {
        assertFalse(paths.canDelete("$root/Keep"))
        assertFalse(paths.canDelete("$root/Keep/a/b.txt"))
        assertTrue(paths.canDelete("$root/Keeper/b.txt"))
    }
}
