package com.apexus.storagelens.di

import javax.inject.Qualifier

/** Portée de coroutines liée au processus : survit aux écrans (scan, suppression). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
