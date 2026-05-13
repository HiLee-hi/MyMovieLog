package com.mymovie.log.presentation.navigation

import android.net.Uri
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

    // Calendar and MovieDetail are hidden from BottomNav
    val showBottomBar = currentDestination?.route !in setOf(
        Screen.Calendar.route,
        Screen.MovieDetail.route,
        Screen.Camera.route,
        Screen.AlbumPicker.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
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
            composable(Screen.Home.route) { navBackStackEntry ->
                LaunchedEffect(Unit) { AppLogger.d("NAVIGATION", "Screen: Home") }
                val homeViewModel: HomeViewModel = hiltViewModel()
                val existingPhotoSignedUrlsHome by homeViewModel.existingPhotoSignedUrls.collectAsStateWithLifecycle()
                val existingPhotoSourceUrisHome by homeViewModel.existingPhotoSourceUris.collectAsStateWithLifecycle()

                val capturedPhotoUriHome by navBackStackEntry.savedStateHandle
                    .getStateFlow("capturedPhotoUri", "")
                    .collectAsStateWithLifecycle()
                LaunchedEffect(capturedPhotoUriHome) {
                    if (capturedPhotoUriHome.isNotBlank()) {
                        AppLogger.d("NAVIGATION", "Home: Camera result received: $capturedPhotoUriHome")
                        homeViewModel.addPhoto(Uri.parse(capturedPhotoUriHome))
                        navBackStackEntry.savedStateHandle["capturedPhotoUri"] = ""
                    }
                }

                val selectedPhotosHome by navBackStackEntry.savedStateHandle
                    .getStateFlow<List<String>>("selectedPhotos", emptyList())
                    .collectAsStateWithLifecycle()
                LaunchedEffect(selectedPhotosHome) {
                    if (selectedPhotosHome.isNotEmpty()) {
                        val existingSourceUris = homeViewModel.existingPhotoSourceUris.value
                        val existingSignedUrls = homeViewModel.existingPhotoSignedUrls.value
                        val returnedSet = selectedPhotosHome.toSet()
                        existingSourceUris.forEachIndexed { index, sourceUri ->
                            if (sourceUri !in returnedSet && index < existingSignedUrls.size) {
                                homeViewModel.removeExistingPhoto(existingSignedUrls[index])
                            }
                        }
                        val newUris = selectedPhotosHome
                            .map { Uri.parse(it) }
                            .filter { it.toString() !in existingSourceUris.toSet() }
                        AppLogger.d("NAVIGATION", "Home: AlbumPicker result: ${selectedPhotosHome.size} total, ${newUris.size} new")
                        homeViewModel.setPhotos(newUris)
                        navBackStackEntry.savedStateHandle["selectedPhotos"] = emptyList<String>()
                    }
                }

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
                        val savedSourceUris = existingPhotoSourceUrisHome.map { Uri.parse(it) }
                        val combined = alreadyAttached + savedSourceUris
                        val unknownCount = (existingPhotoSignedUrlsHome.size - existingPhotoSourceUrisHome.size).coerceAtLeast(0)
                        AppLogger.d("NAVIGATION", "Home → AlbumPicker: ${combined.size} attached, $unknownCount unknown")
                        navBackStackEntry.savedStateHandle["alreadyAttachedUris"] = combined.map { it.toString() }
                        navBackStackEntry.savedStateHandle["existingPhotoCount"] = unknownCount
                        navController.navigate(Screen.AlbumPicker.route)
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
                    }
                )
            }
            composable(
                route = Screen.MovieDetail.route,
                arguments = listOf(navArgument(Screen.MovieDetail.ARG_MOVIE_ID) { type = NavType.IntType })
            ) { navBackStackEntry ->
                LaunchedEffect(Unit) { AppLogger.d("NAVIGATION", "Screen: MovieDetail") }

                val movieDetailViewModel: MovieDetailViewModel = hiltViewModel()
                val existingPhotoSignedUrlsDetail by movieDetailViewModel.existingPhotoSignedUrls.collectAsStateWithLifecycle()
                val existingPhotoSourceUrisDetail by movieDetailViewModel.existingPhotoSourceUris.collectAsStateWithLifecycle()

                val capturedPhotoUri by navBackStackEntry.savedStateHandle
                    .getStateFlow("capturedPhotoUri", "")
                    .collectAsStateWithLifecycle()
                LaunchedEffect(capturedPhotoUri) {
                    if (capturedPhotoUri.isNotBlank()) {
                        AppLogger.d("NAVIGATION", "Camera result received: $capturedPhotoUri")
                        movieDetailViewModel.addPhoto(Uri.parse(capturedPhotoUri))
                        navBackStackEntry.savedStateHandle["capturedPhotoUri"] = ""
                    }
                }

                val selectedPhotos by navBackStackEntry.savedStateHandle
                    .getStateFlow<List<String>>("selectedPhotos", emptyList())
                    .collectAsStateWithLifecycle()
                LaunchedEffect(selectedPhotos) {
                    if (selectedPhotos.isNotEmpty()) {
                        val existingSourceUris = movieDetailViewModel.existingPhotoSourceUris.value
                        val existingSignedUrls = movieDetailViewModel.existingPhotoSignedUrls.value
                        val returnedSet = selectedPhotos.toSet()
                        existingSourceUris.forEachIndexed { index, sourceUri ->
                            if (sourceUri !in returnedSet && index < existingSignedUrls.size) {
                                movieDetailViewModel.removeExistingPhoto(existingSignedUrls[index])
                            }
                        }
                        val newUris = selectedPhotos
                            .map { Uri.parse(it) }
                            .filter { it.toString() !in existingSourceUris.toSet() }
                        AppLogger.d("NAVIGATION", "MovieDetail: AlbumPicker result: ${selectedPhotos.size} total, ${newUris.size} new")
                        movieDetailViewModel.setPhotos(newUris)
                        navBackStackEntry.savedStateHandle["selectedPhotos"] = emptyList<String>()
                    }
                }

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
                        val savedSourceUris = existingPhotoSourceUrisDetail.map { Uri.parse(it) }
                        val combined = alreadyAttached + savedSourceUris
                        val unknownCount = (existingPhotoSignedUrlsDetail.size - existingPhotoSourceUrisDetail.size).coerceAtLeast(0)
                        AppLogger.d("NAVIGATION", "MovieDetail → AlbumPicker: ${combined.size} attached, $unknownCount unknown")
                        navBackStackEntry.savedStateHandle["alreadyAttachedUris"] = combined.map { it.toString() }
                        navBackStackEntry.savedStateHandle["existingPhotoCount"] = unknownCount
                        navController.navigate(Screen.AlbumPicker.route)
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
                val existingPhotoSignedUrlsLibrary by libraryViewModel.existingPhotoSignedUrls.collectAsStateWithLifecycle()
                val existingPhotoSourceUrisLibrary by libraryViewModel.existingPhotoSourceUris.collectAsStateWithLifecycle()

                val capturedPhotoUriLibrary by navBackStackEntry.savedStateHandle
                    .getStateFlow("capturedPhotoUri", "")
                    .collectAsStateWithLifecycle()
                LaunchedEffect(capturedPhotoUriLibrary) {
                    if (capturedPhotoUriLibrary.isNotBlank()) {
                        AppLogger.d("NAVIGATION", "Library: Camera result received: $capturedPhotoUriLibrary")
                        libraryViewModel.addPhoto(Uri.parse(capturedPhotoUriLibrary))
                        navBackStackEntry.savedStateHandle["capturedPhotoUri"] = ""
                    }
                }

                val selectedPhotosLibrary by navBackStackEntry.savedStateHandle
                    .getStateFlow<List<String>>("selectedPhotos", emptyList())
                    .collectAsStateWithLifecycle()
                LaunchedEffect(selectedPhotosLibrary) {
                    if (selectedPhotosLibrary.isNotEmpty()) {
                        val existingSourceUris = libraryViewModel.existingPhotoSourceUris.value
                        val existingSignedUrls = libraryViewModel.existingPhotoSignedUrls.value
                        val returnedSet = selectedPhotosLibrary.toSet()
                        existingSourceUris.forEachIndexed { index, sourceUri ->
                            if (sourceUri !in returnedSet && index < existingSignedUrls.size) {
                                libraryViewModel.removeExistingPhoto(existingSignedUrls[index])
                            }
                        }
                        val newUris = selectedPhotosLibrary
                            .map { Uri.parse(it) }
                            .filter { it.toString() !in existingSourceUris.toSet() }
                        AppLogger.d("NAVIGATION", "Library: AlbumPicker result: ${selectedPhotosLibrary.size} total, ${newUris.size} new")
                        libraryViewModel.setPhotos(newUris)
                        navBackStackEntry.savedStateHandle["selectedPhotos"] = emptyList<String>()
                    }
                }

                LibraryScreen(
                    isLoggedIn = isLoggedIn,
                    onNavigateToLogin = navigateToProfile,
                    onOpenCamera = {
                        AppLogger.d("NAVIGATION", "Library → Camera")
                        navController.navigate(Screen.Camera.route)
                    },
                    onOpenAlbumPicker = { alreadyAttached ->
                        val savedSourceUris = existingPhotoSourceUrisLibrary.map { Uri.parse(it) }
                        val combined = alreadyAttached + savedSourceUris
                        val unknownCount = (existingPhotoSignedUrlsLibrary.size - existingPhotoSourceUrisLibrary.size).coerceAtLeast(0)
                        AppLogger.d("NAVIGATION", "Library → AlbumPicker: ${combined.size} attached, $unknownCount unknown")
                        navBackStackEntry.savedStateHandle["alreadyAttachedUris"] = combined.map { it.toString() }
                        navBackStackEntry.savedStateHandle["existingPhotoCount"] = unknownCount
                        navController.navigate(Screen.AlbumPicker.route)
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
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "capturedPhotoUri", uri.toString()
                        )
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
                    ?.get<List<String>>("alreadyAttachedUris")
                    ?: emptyList()
                val alreadyAttached = remember(alreadyAttachedStrings) {
                    alreadyAttachedStrings.map { Uri.parse(it) }
                }
                val existingPhotoCount = navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<Int>("existingPhotoCount")
                    ?: 0
                AlbumPickerScreen(
                    alreadyAttachedUris = alreadyAttached,
                    existingPhotoCount = existingPhotoCount,
                    onConfirm = { selectedUris ->
                        AppLogger.d("NAVIGATION", "AlbumPicker → confirmed ${selectedUris.size} photos, pop back")
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "selectedPhotos", selectedUris.map { it.toString() }
                        )
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
