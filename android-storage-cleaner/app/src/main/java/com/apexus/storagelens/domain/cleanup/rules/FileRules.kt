package com.apexus.storagelens.domain.cleanup.rules

import com.apexus.storagelens.domain.cleanup.FileRule
import com.apexus.storagelens.domain.cleanup.PathPatterns
import com.apexus.storagelens.domain.cleanup.RuleContext
import com.apexus.storagelens.domain.cleanup.candidate
import com.apexus.storagelens.domain.model.CleanupCandidate
import com.apexus.storagelens.domain.model.CleanupCategory
import com.apexus.storagelens.domain.scan.ScannedFile

class LogFileRule : FileRule {
    override val category = CleanupCategory.LOGS
    override fun match(file: ScannedFile, ctx: RuleContext): CleanupCandidate? =
        if (PathPatterns.isLogFile(file.name, PathPatterns.relativeTo(file.parentPath, ctx.storageRoot))) file.candidate(category) else null
}

class TraceFileRule : FileRule {
    override val category = CleanupCategory.TRACES
    override fun match(file: ScannedFile, ctx: RuleContext): CleanupCandidate? =
        if (PathPatterns.isTraceFile(file.name, PathPatterns.relativeTo(file.parentPath, ctx.storageRoot))) file.candidate(category) else null
}

class TempFileRule : FileRule {
    override val category = CleanupCategory.TEMP_FILES
    override fun match(file: ScannedFile, ctx: RuleContext): CleanupCandidate? {
        if (!PathPatterns.isTempFile(file.name)) return null
        // Un fichier en cours d'écriture (modifié il y a moins d'une heure) n'est pas proposé.
        if (ctx.nowMillis - file.lastModified < RECENT_WRITE_MS) return null
        val backup = PathPatterns.isBackupFile(file.name)
        return file.candidate(category, recommended = !backup, detail = if (backup) "backup" else null)
    }

    companion object {
        const val RECENT_WRITE_MS = 60L * 60 * 1000
    }
}

class ApkFileRule : FileRule {
    override val category = CleanupCategory.APK_FILES
    override fun match(file: ScannedFile, ctx: RuleContext): CleanupCandidate? {
        if (PathPatterns.extension(file.name) !in APK_EXTENSIONS) return null
        val pkg = ctx.apkPackageResolver(file.path)
        val installed = pkg != null && pkg in ctx.installedPackages
        return file.candidate(category, recommended = installed, detail = if (installed) "installed" else pkg)
    }

    companion object {
        val APK_EXTENSIONS = setOf("apk", "xapk", "apks", "apkm")
    }
}

class MediaStoreTrashRule : FileRule {
    override val category = CleanupCategory.TRASH
    override fun match(file: ScannedFile, ctx: RuleContext): CleanupCandidate? =
        if (PathPatterns.isMediaStoreTrashed(file.name)) file.candidate(category) else null
}

class LargeOldFileRule : FileRule {
    override val category = CleanupCategory.LARGE_OLD_FILES
    override fun match(file: ScannedFile, ctx: RuleContext): CleanupCandidate? {
        val old = ctx.nowMillis - file.lastModified >= ctx.oldFileAgeMillis
        return if (file.size >= ctx.largeFileThresholdBytes && old) file.candidate(category) else null
    }
}
