package com.apexus.storagelens.domain.cleanup

import com.apexus.storagelens.domain.scan.ScannedFile
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.security.MessageDigest

/**
 * Détection de doublons en trois passes : même taille → empreinte partielle
 * (début + fin du fichier) → SHA-256 complet.
 */
class DuplicateFinder(private val partialChunk: Int = 64 * 1024) {

    suspend fun findDuplicates(sameSizeGroups: List<List<ScannedFile>>): List<List<ScannedFile>> {
        val result = ArrayList<List<ScannedFile>>()
        for (group in sameSizeGroups) {
            if (group.size < 2) continue
            val byPartial = group.groupBy { file ->
                currentCoroutineContext().ensureActive()
                partialHash(file.path)
            }.filterKeys { it != null }.values.filter { it.size >= 2 }

            for (candidates in byPartial) {
                val needsFullHash = candidates.first().size > 2L * partialChunk
                val confirmed = if (needsFullHash) {
                    candidates.groupBy { file ->
                        currentCoroutineContext().ensureActive()
                        fullHash(file.path)
                    }.filterKeys { it != null }.values.filter { it.size >= 2 }
                } else {
                    listOf(candidates) // l'empreinte partielle couvre déjà tout le fichier
                }
                result += confirmed
            }
        }
        return result
    }

    internal fun partialHash(path: String): String? = try {
        RandomAccessFile(File(path), "r").use { raf ->
            val digest = MessageDigest.getInstance("SHA-256")
            val length = raf.length()
            val buffer = ByteArray(partialChunk)
            val headRead = raf.read(buffer, 0, minOf(partialChunk.toLong(), length).toInt())
            if (headRead > 0) digest.update(buffer, 0, headRead)
            if (length > partialChunk) {
                val tailStart = maxOf(partialChunk.toLong(), length - partialChunk)
                raf.seek(tailStart)
                val tailRead = raf.read(buffer, 0, (length - tailStart).toInt())
                if (tailRead > 0) digest.update(buffer, 0, tailRead)
            }
            digest.update(length.toString().toByteArray())
            digest.digest().toHex()
        }
    } catch (e: IOException) {
        null
    }

    internal suspend fun fullHash(path: String): String? = try {
        File(path).inputStream().buffered(256 * 1024).use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(256 * 1024)
            var chunks = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
                if (++chunks % 16 == 0) currentCoroutineContext().ensureActive()
            }
            digest.digest().toHex()
        }
    } catch (e: IOException) {
        null
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
