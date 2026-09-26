package com.apexus.storagelens.domain.scan

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/** Compteurs partagés, lus périodiquement par l'appelant pour publier la progression. */
class ScanCounters {
    val files = AtomicLong()
    val bytes = AtomicLong()
    val directories = AtomicLong()
    val inaccessibleDirectories = AtomicInteger()

    @Volatile
    var currentPath: String = ""
}
