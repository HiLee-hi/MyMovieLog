package com.mymovie.log.presentation.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.bundleOf
import androidx.hilt.navigation.HiltViewModelFactory
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import com.mymovie.log.presentation.adaptive.LocalAdaptiveLayoutInfo
import com.mymovie.log.presentation.adaptive.NavigationLayout
import com.mymovie.log.presentation.adaptive.ProvideAdaptiveLayoutInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mymovie.log.domain.model.WatchStatus
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mymovie.log.presentation.search.SearchViewModel
import com.mymovie.log.presentation.calendar.CalendarScreen
import com.mymovie.log.presentation.detail.MovieDetailScreen
import com.mymovie.log.presentation.home.HomeScreen
import com.mymovie.log.presentation.home.HomeViewModel
import com.mymovie.log.presentation.library.LibraryScreen
import com.mymovie.log.presentation.library.LibraryViewModel
import com.mymovie.log.presentation.profile.ProfileScreen
import com.mymovie.log.presentation.camera.CameraScreen
import com.mymovie.log.presentation.detail.MovieDetailViewModel
import com.mymovie.log.presentation.picker.AlbumPickerScreen
import com.mymovie.log.presentation.search.SearchScreen
import com.mymovie.log.presentation.stats.StatsScreen
import com.mymovie.log.util.AppLogger

sealed class Screen(val route: String) {
    open val clickRoute: String get() = route

    object Home : Screen("home")
    object Search : Screen("search")
    object Library : Screen("library?tab={tab}") {
        override val clickRoute = "library"
        const val ARG_TAB = "tab"
        fun createRoute(tab: String? = null) = if (tab != null) "library?tab=$tab" else "library"
    }
    object Calendar : Screen("calendar")
    object Stats : Screen("stats")
    object Profile : Screen("profile")
    object MovieDetail : Screen("movie_detail/{movieId}") {
        const val ARG_MOVIE_ID = "movieId"
        fun createRoute(movieId: Int) = "movie_detail/$movieId"
    }
    object Camera : Screen("camera")
    object AlbumPicker : Screen("album_picker")
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

fun navigateToLibraryTab(navController: androidx.navigation.NavController, tab: WatchStatus) {
    AppLogger.d("NAVIGATION", "Navigate to Library tab: ${tab.value}")
    navController.navigate(Screen.Library.createRoute(tab.value)) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = false
    }
}

@Composable
fun AppNavHost(appViewModel: AppViewModel = hiltViewModel()) {
    // Read once for the whole app; recomputed on every window change (fold, rotate, resize)
    ProvideAdaptiveLayoutInfo {
        AppNavHostContent(appViewModel)
    }
}

@Composable
private fun AppNavHostContent(appViewModel: AppViewModel) {
    // The NavController lives above the adaptive navigation UI, so switching between the bottom
    // bar and the rail never resets the current destination or the back stack.
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isLoggedIn by appViewModel.isLoggedIn.collectAsStateWithLifecycle()
    val adaptiveInfo = LocalAdaptiveLayoutInfo.current

    val navigateToProfile: () -> Unit = {
        AppLogger.i("NAVIGATION", "LoginRequired → navigate to Profile")
        navController.navigate(Screen.Profile.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Calendar and MovieDetail are hidden from BottomNav
    val showBottomBar = currentDestination?.route !in setOf(
        Screen.Calendar.route,
        Screen.MovieDetail.route,
        Screen.Camera.route,
        Screen.AlbumPicker.route
    )

    // Bar on compact windows (original), rail when there is room beside the content
    val navigationSuiteType = when {
        !showBottomBar -> NavigationSuiteType.None
        adaptiveInfo.navigationLayout == NavigationLayout.Rail -> NavigationSuiteType.NavigationRail
        else -> NavigationSuiteType.NavigationBar
    }

    NavigationSuiteScaffold(
        layoutType = navigationSuiteType,
        navigationSuiteItems = {
            bottomNavItems.forEach { item ->
                item(
                    selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                    onClick = {
                        AppLogger.d("NAVIGATION", "BottomNav tab: ${item.screen.route}")
                        navController.navigate(item.screen.clickRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = {
                        // Narrow cover screens leave less than the label's width inside the
                        // item's padding; keep one line, centered over the whole item.
                        Text(
                            text = item.label,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.wrapContentWidth(unbounded = true)
                        )
                    }
                )
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            // The bar consumes the bottom inset itself; with the rail or no navigation the
            // content keeps clear of the system navigation bar
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.End)
            )
        ) {
            composable(Screen.Home.route) { navBackStackEntry ->
                LaunchedEffect(Unit) { AppLogger.d("NAVIGATION", "Screen: Home") }
                val homeViewModel: HomeViewModel = hiltViewModel()
                PhotoPickerResultEffect(navBackStackEntry.savedStateHandle, homeViewModel, "Home")

                HomeScreen(
                    onNavigateToCalendar = {
                        AppLogger.d("NAVIGATION", "Home → Calendar")
                        navController.navigate(Screen.Calendar.route)
                    },
                    onNavigateToLibraryTab = { tab ->
                        navigateToLibraryTab(navController, tab)
                    },
                    onOpenCamera = {
                        AppLogger.d("NAVIGATION", "Home → Camera")
                        navController.navigate(Screen.Camera.route)
                    },
                    onOpenAlbumPicker = { alreadyAttached ->
                        openAlbumPicker(navController, navBackStackEntry.savedStateHandle, homeViewModel, alreadyAttached, "Home")
                    },
                    viewModel = homeViewModel
                )
            }
            composable(Screen.Search.route) { searchEntry ->
                LaunchedEffect(Unit) { AppLogger.d("NAVIGATION", "Screen: Search") }
                val searchViewModel: SearchViewModel = hiltViewModel()
                val recordSaved by remember {
                    searchEntry.savedStateHandle.getStateFlow("recordSaved", false)
                }.collectAsStateWithLifecycle()
                LaunchedEffect(recordSaved) {
                    if (recordSaved) {
                        searchViewModel.clearSearch()
                        searchEntry.savedStateHandle["recordSaved"] = false
                    }
                }
                SearchScreen(
                    viewModel = searchViewModel,
                    onMovieClick = { movieId ->
                        AppLogger.d("NAVIGATION", "Search → MovieDetail: movieId=$movieId")
                        navController.navigate(Screen.MovieDetail.createRoute(movieId))
                    },
                    detailPane = { movieId, isSinglePane, onClose ->
                        // Two-pane detail: same screen and ViewModel as the MovieDetail destination,
                        // scoped to the Search entry so it survives window changes
                        val detailViewModel = movieDetailPaneViewModel(searchEntry, movieId)
                        PhotoPickerResultEffect(searchEntry.savedStateHandle, detailViewModel, "SearchDetailPane")
                        MovieDetailScreen(
                            onBack = { onClose() },
                            isLoggedIn = isLoggedIn,
                            onNavigateToLogin = navigateToProfile,
                            onOpenCamera = {
                                AppLogger.d("NAVIGATION", "SearchDetailPane → Camera")
                                navController.navigate(Screen.Camera.route)
                            },
                            onOpenAlbumPicker = { alreadyAttached ->
                                openAlbumPicker(navController, searchEntry.savedStateHandle, detailViewModel, alreadyAttached, "SearchDetailPane")
                            },
                            showNavigationIcon = isSinglePane,
                            handleSystemBack = false,
                            viewModel = detailViewModel
                        )
                    }
                )
            }
            composable(
                route = Screen.MovieDetail.route,
                arguments = listOf(navArgument(Screen.MovieDetail.ARG_MOVIE_ID) { type = NavType.IntType })
            ) { navBackStackEntry ->
                LaunchedEffect(Unit) { AppLogger.d("NAVIGATION", "Screen: MovieDetail") }

                val movieDetailViewModel: MovieDetailViewModel = hiltViewModel()
                PhotoPickerResultEffect(navBackStackEntry.savedStateHandle, movieDetailViewModel, "MovieDetail")

                MovieDetailScreen(
                    onBack = { recordSaved ->
                        AppLogger.d("NAVIGATION", "MovieDetail → Back (recordSaved=$recordSaved)")
                        if (recordSaved) {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("recordSaved", true)
                        }
                        navController.popBackStack()
                    },
                    isLoggedIn = isLoggedIn,
                    onNavigateToLogin = navigateToProfile,
                    onOpenCamera = {
                        AppLogger.d("NAVIGATION", "MovieDetail → Camera")
                        navController.navigate(Screen.Camera.route)
                    },
                    onOpenAlbumPicker = { alreadyAttached ->
                        openAlbumPicker(navController, navBackStackEntry.savedStateHandle, movieDetailViewModel, alreadyAttached, "MovieDetail")
                    },
                    viewModel = movieDetailViewModel
                )
            }
            composable(
                route = Screen.Library.route,
                arguments = listOf(navArgument(Screen.Library.ARG_TAB) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { navBackStackEntry ->
                LaunchedEffect(Unit) { AppLogger.d("NAVIGATION", "Screen: Library") }
                val libraryViewModel: LibraryViewModel = hiltViewModel()
                PhotoPickerResultEffect(navBackStackEntry.savedStateHandle, libraryViewModel, "Library")

                LibraryScreen(
                    isLoggedIn = isLoggedIn,
                    onNavigateToLogin = navigateToProfile,
                    onOpenCamera = {
                        AppLogger.d("NAVIGATION", "Library → Camera")
                        navController.navigate(Screen.Camera.route)
                    },
                    onOpenAlbumPicker = { alreadyAttached ->
                        openAlbumPicker(navController, navBackStackEntry.savedStateHandle, libraryViewModel, alreadyAttached, "Library")
                    },
                    viewModel = libraryViewModel
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
            composable(Screen.Camera.route) {
                LaunchedEffect(Unit) { AppLogger.d("NAVIGATION", "Screen: Camera") }
                CameraScreen(
                    onPhotoTaken = { uri ->
                        AppLogger.d("NAVIGATION", "Camera → photo taken, pop back")
                        deliverCapturedPhoto(navController, uri)
                        navController.popBackStack()
                    },
                    onBack = {
                        AppLogger.d("NAVIGATION", "Camera → Back")
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.AlbumPicker.route) {
                LaunchedEffect(Unit) { AppLogger.d("NAVIGATION", "Screen: AlbumPicker") }
                val alreadyAttachedStrings = navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<List<String>>(KEY_ALREADY_ATTACHED_URIS)
                    ?: emptyList()
                val alreadyAttached = remember(alreadyAttachedStrings) {
                    alreadyAttachedStrings.map { Uri.parse(it) }
                }
                val existingPhotoCount = navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<Int>(KEY_EXISTING_PHOTO_COUNT)
                    ?: 0
                AlbumPickerScreen(
                    alreadyAttachedUris = alreadyAttached,
                    existingPhotoCount = existingPhotoCount,
                    onConfirm = { selectedUris ->
                        AppLogger.d("NAVIGATION", "AlbumPicker → confirmed ${selectedUris.size} photos, pop back")
                        deliverSelectedPhotos(navController, selectedUris)
                        navController.popBackStack()
                    },
                    onBack = {
                        AppLogger.d("NAVIGATION", "AlbumPicker → Back")
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

/**
 * MovieDetailViewModel for the Search detail pane. It is stored in the Search back stack entry
 * (one instance per movie, keyed by id) and receives `movieId` through the same SavedStateHandle
 * argument the MovieDetail destination uses, so the ViewModel itself is unchanged.
 */
@Composable
private fun movieDetailPaneViewModel(entry: NavBackStackEntry, movieId: Int): MovieDetailViewModel {
    val context = LocalContext.current
    val extras = remember(entry, movieId) {
        MutableCreationExtras(entry.defaultViewModelCreationExtras).apply {
            set(DEFAULT_ARGS_KEY, bundleOf(Screen.MovieDetail.ARG_MOVIE_ID to movieId))
        }
    }
    return viewModel(
        viewModelStoreOwner = entry,
        key = "search_detail_pane_$movieId",
        factory = HiltViewModelFactory(context, entry),
        extras = extras
    )
}
