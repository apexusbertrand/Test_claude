package com.apexus.storagelens.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.apexus.storagelens.R
import com.apexus.storagelens.ui.screens.apps.AppsScreen
import com.apexus.storagelens.ui.screens.cleanup.CleanupScreen
import com.apexus.storagelens.ui.screens.explorer.ExplorerScreen
import com.apexus.storagelens.ui.screens.history.HistoryScreen
import com.apexus.storagelens.ui.screens.home.HomeScreen
import com.apexus.storagelens.ui.screens.onboarding.OnboardingScreen
import com.apexus.storagelens.ui.screens.onboarding.PermissionsScreen
import com.apexus.storagelens.ui.screens.scan.ScanScreen
import com.apexus.storagelens.ui.screens.settings.AboutScreen
import com.apexus.storagelens.ui.screens.settings.SettingsScreen
import com.apexus.storagelens.ui.screens.trash.TrashScreen
import kotlin.reflect.KClass

private data class TopLevel(val route: Any, val routeClass: KClass<*>, val icon: ImageVector, @StringRes val label: Int)

private val TOP_LEVEL = listOf(
    TopLevel(HomeRoute, HomeRoute::class, Icons.Filled.Home, R.string.nav_home),
    TopLevel(ExplorerRoute, ExplorerRoute::class, Icons.Filled.FolderOpen, R.string.nav_explorer),
    TopLevel(CleanupRoute, CleanupRoute::class, Icons.Filled.CleaningServices, R.string.nav_cleanup),
    TopLevel(AppsRoute(), AppsRoute::class, Icons.Filled.Apps, R.string.nav_apps),
)

@Composable
fun AppNavHost(showOnboarding: Boolean, navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = TOP_LEVEL.any { top -> destination?.hierarchy?.any { it.hasRoute(top.routeClass) } == true }

    fun navigateTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(HomeRoute) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun startScan() {
        navController.navigate(ScanRoute) { launchSingleTop = true }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TOP_LEVEL.forEach { top ->
                        val selected = destination?.hierarchy?.any { it.hasRoute(top.routeClass) } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateTopLevel(top.route) },
                            icon = { Icon(top.icon, contentDescription = null) },
                            label = { Text(stringResource(top.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (showOnboarding) OnboardingRoute else HomeRoute,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable<OnboardingRoute> {
                OnboardingScreen(onFinished = { navController.navigate(PermissionsRoute(fromOnboarding = true)) })
            }
            composable<PermissionsRoute> { entry ->
                val route = entry.toRoute<PermissionsRoute>()
                PermissionsScreen(
                    fromOnboarding = route.fromOnboarding,
                    onDone = {
                        navController.navigate(HomeRoute) {
                            popUpTo(OnboardingRoute) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<HomeRoute> {
                HomeScreen(
                    onStartScan = ::startScan,
                    onOpenCleanup = { navigateTopLevel(CleanupRoute) },
                    onOpenExplorer = { navigateTopLevel(ExplorerRoute) },
                    onOpenPermissions = { navController.navigate(PermissionsRoute()) },
                    onOpenTrash = { navController.navigate(TrashRoute) },
                    onOpenHistory = { navController.navigate(HistoryRoute) },
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                    onOpenAbout = { navController.navigate(AboutRoute) },
                )
            }
            composable<ScanRoute> {
                ScanScreen(onFinished = { navController.popBackStack() })
            }
            composable<ExplorerRoute> {
                ExplorerScreen(onStartScan = { navigateTopLevel(HomeRoute) })
            }
            composable<CleanupRoute> {
                CleanupScreen(
                    onStartScan = { navigateTopLevel(HomeRoute) },
                    onOpenAssistedCache = { navController.navigate(AppsRoute(sortByCache = true)) },
                    onOpenPermissions = { navController.navigate(PermissionsRoute()) },
                )
            }
            composable<AppsRoute> {
                AppsScreen(onOpenPermissions = { navController.navigate(PermissionsRoute()) })
            }
            composable<TrashRoute> { TrashScreen(onBack = { navController.popBackStack() }) }
            composable<HistoryRoute> { HistoryScreen(onBack = { navController.popBackStack() }) }
            composable<SettingsRoute> {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPermissions = { navController.navigate(PermissionsRoute()) },
                    onOpenAbout = { navController.navigate(AboutRoute) },
                )
            }
            composable<AboutRoute> { AboutScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
