package com.apexus.storagelens.domain

import java.io.File
import java.nio.file.Files

/** Petit utilitaire pour construire une arborescence de test. */
class TestFs(val root: File = Files.createTempDirectory("storagelens").toFile()) {
    fun file(relative: String, size: Int = 10, ageMillis: Long = 0L, content: ByteArray? = null): File {
        val f = File(root, relative)
        f.parentFile.mkdirs()
        f.writeBytes(content ?: ByteArray(size) { (it % 251).toByte() })
        if (ageMillis > 0) f.setLastModified(System.currentTimeMillis() - ageMillis)
        return f
    }

    fun dir(relative: String): File = File(root, relative).also { it.mkdirs() }

    fun path(relative: String): String = File(root, relative).path

    fun delete() = root.deleteRecursively()
}
