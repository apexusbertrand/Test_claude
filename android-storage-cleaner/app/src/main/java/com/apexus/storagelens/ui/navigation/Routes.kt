package com.apexus.storagelens.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object OnboardingRoute
@Serializable data class PermissionsRoute(val fromOnboarding: Boolean = false)
@Serializable data object HomeRoute
@Serializable data object ScanRoute
@Serializable data object ExplorerRoute
@Serializable data object CleanupRoute
@Serializable data class AppsRoute(val sortByCache: Boolean = false)
@Serializable data object TrashRoute
@Serializable data object HistoryRoute
@Serializable data object SettingsRoute
@Serializable data object AboutRoute
@Serializable data object SystemStorageRoute
