package com.mymovie.log.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mymovie.log.presentation.calendar.CalendarScreen
import com.mymovie.log.presentation.home.HomeScreen
import com.mymovie.log.presentation.library.LibraryScreen
import com.mymovie.log.presentation.profile.ProfileScreen
import com.mymovie.log.presentation.search.SearchScreen
import com.mymovie.log.presentation.stats.StatsScreen
import com.mymovie.log.util.AppLogger

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Library : Screen("library")
    object Calendar : Screen("calendar")
    object Stats : Screen("stats")
    object Profile : Screen("profile")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "홈", Icons.Default.Home),
    BottomNavItem(Screen.Search, "검색", Icons.Default.Search),
    BottomNavItem(Screen.Library, "라이브러리", Icons.Default.LocalMovies),
    BottomNavItem(Screen.Stats, "통계", Icons.Default.BarChart),
    BottomNavItem(Screen.Profile, "프로필", Icons.Default.Person)
)

@Composable
fun AppNavHost(appViewModel: AppViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isLoggedIn by appViewModel.isLoggedIn.collectAsStateWithLifecycle()

    val navigateToProfile: () -> Unit = {
        AppLogger.i("NAVIGATION", "LoginRequired → navigate to Profile")
        navController.navigate(Screen.Profile.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Calendar is hidden from BottomNav (accessed via button on Home screen)
    val showBottomBar = currentDestination?.route != Screen.Calendar.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                            onClick = {
                                AppLogger.d("NAVIGATION", "BottomNav tab: ${item.screen.route}")
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                LaunchedEffect(Unit) { AppLogger.d("NAVIGATION", "Screen: Home") }
                HomeScreen(onNavigateToCalendar = {
                    AppLogger.d("NAVIGATION", "Home → Calendar")
                    navController.navigate(Screen.Calendar.route)
                })
            }
            composable(Screen.Search.route) {
                LaunchedEffect(Unit) { AppLogger.d("NAVIGATION", "Screen: Search") }
                SearchScreen(
                    isLoggedIn = isLoggedIn,
                    onNavigateToLogin = navigateToProfile
                )
            }
            composable(Screen.Library.route) {
                LaunchedEffect(Unit) { AppLogger.d("NAVIGATION", "Screen: Library") }
                LibraryScreen(
                    isLoggedIn = isLoggedIn,
                    onNavigateToLogin = navigateToProfile
                )
            }
            composable(Screen.Calendar.route) {
                LaunchedEffect(Unit) { AppLogger.d("NAVIGATION", "Screen: Calendar") }
                CalendarScreen(
                    onBack = {
                        AppLogger.d("NAVIGATION", "Calendar → Back")
                        navController.popBackStack()
                    },
                    isLoggedIn = isLoggedIn,
                    onNavigateToLogin = navigateToProfile
                )
            }
            composable(Screen.Stats.route) {
                LaunchedEffect(Unit) { AppLogger.d("NAVIGATION", "Screen: Stats") }
                StatsScreen(
                    isLoggedIn = isLoggedIn,
                    onNavigateToLogin = navigateToProfile
                )
            }
            composable(Screen.Profile.route) {
                LaunchedEffect(Unit) { AppLogger.d("NAVIGATION", "Screen: Profile") }
                ProfileScreen(
                    onLoginSuccess = {
                        AppLogger.i("NAVIGATION", "Login success → navigate to Home")
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}
