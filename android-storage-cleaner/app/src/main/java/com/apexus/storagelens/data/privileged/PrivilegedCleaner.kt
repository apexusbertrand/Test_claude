package com.apexus.storagelens.data.privileged

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Zone système accessible uniquement avec des droits élevés. */
data class PrivilegedArea(val path: String, val sizeBytes: Long?)

/**
 * Nettoyage des journaux système (`/data/log`, `/data/tombstones`, `/data/anr`,
 * `/data/system/dropbox`). Impossible sans droits élevés : l'implémentation fournie
 * utilise `su` (appareil rooté). Désactivée par défaut, activable en mode avancé.
 */
interface PrivilegedCleaner {
    suspend fun isAvailable(): Boolean
    suspend fun measure(): List<PrivilegedArea>
    suspend fun clean(): Boolean
}

@Singleton
class RootCleaner @Inject constructor() : PrivilegedCleaner {

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        run("id").let { it != null && it.contains("uid=0") }
    }

    override suspend fun measure(): List<PrivilegedArea> = withContext(Dispatchers.IO) {
        SYSTEM_AREAS.map { path ->
            val output = run("du -sk $path")
            val kb = output?.trim()?.split(Regex("\\s+"))?.firstOrNull()?.toLongOrNull()
            PrivilegedArea(path, kb?.times(1024))
        }
    }

    override suspend fun clean(): Boolean = withContext(Dispatchers.IO) {
        // Supprime uniquement le contenu de dossiers fixes et connus, jamais les dossiers eux-mêmes.
        val command = SYSTEM_AREAS.joinToString(" ; ") { "find $it -mindepth 1 -delete" }
        run(command) != null
    }

    private fun run(command: String): String? = try {
        val process = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroy()
            null
        } else if (process.exitValue() == 0) output else null
    } catch (e: IOException) {
        null
    } catch (e: InterruptedException) {
        null
    }

    companion object {
        val SYSTEM_AREAS = listOf("/data/log", "/data/tombstones", "/data/anr", "/data/system/dropbox")
    }
}
