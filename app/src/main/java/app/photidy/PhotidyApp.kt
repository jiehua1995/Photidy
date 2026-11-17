package app.photidy

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.photidy.core.data.PhotidyGraph
import app.photidy.core.designsystem.components.PhotidyScreen
import app.photidy.core.designsystem.theme.PhotidyTheme
import app.photidy.core.localization.LocaleController
import app.photidy.core.model.AppSettings
import app.photidy.core.model.ThemeMode
import app.photidy.feature.favorites.FavoritesRoute
import app.photidy.feature.filters.FiltersRoute
import app.photidy.feature.home.HomeRoute
import app.photidy.feature.insights.InsightsRoute
import app.photidy.feature.organizer.OrganizerRoute
import app.photidy.feature.photodetail.PhotoDetailRoute
import app.photidy.feature.review.ReviewRoute
import app.photidy.feature.search.SearchRoute
import app.photidy.feature.settings.SettingsRoute

private object Routes {
    const val HOME = "home"
    const val FILTERS = "filters"
    const val ORGANIZER = "organizer"
    const val REVIEW = "review"
    const val FAVORITES = "favorites"
    const val SEARCH = "search"
    const val INSIGHTS = "insights"
    const val SETTINGS = "settings"
    const val PHOTO_DETAIL = "photo/{photoId}"

    fun photoDetail(photoId: String): String = "photo/$photoId"
}

@Composable
fun PhotidyApp() {
    // Null until DataStore delivers the persisted settings; the placeholder must never be
    // written back into the per-app locale or the app would recreate itself in a loop.
    val loadedSettings by PhotidyGraph.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = null,
    )
    if (loadedSettings == null) {
        // DataStore has not delivered the persisted settings yet. Rendering only the themed
        // backdrop keeps the demo library and the default theme from flashing before the
        // saved source, style and language are known.
        PhotidyTheme(settings = AppSettings()) {
            PhotidyScreen(showCloud = false) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(160.dp),
                )
            }
        }
        return
    }
    val settings = loadedSettings ?: AppSettings()
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val view = LocalView.current
    val context = LocalContext.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = context.findActivity()?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    // Keeps AppCompat's persisted per-app locale aligned with DataStore. Only runs once the
    // persisted value is known, so a restore from AppCompat is never undone by a placeholder.
    LaunchedEffect(loadedSettings?.language) {
        loadedSettings?.let { LocaleController.apply(it.language) }
    }

    // Restores the persisted photo source after process death; without this the app would
    // silently fall back to the demo library even though the user chose device photos.
    LaunchedEffect(loadedSettings?.libraryMode) {
        loadedSettings?.let { PhotidyGraph.photoRepository.applyMode(it.libraryMode) }
    }

    PhotidyTheme(settings = settings) {
        PhotidyNavHost()
    }
}

@Composable
private fun PhotidyNavHost() {
    val navController = rememberNavController()
    val animationsEnabled = PhotidyTheme.animationsEnabled
    val duration = PhotidyTheme.motion.durationMedium
    val enter = fadeIn(animationSpec = tween(if (animationsEnabled) duration else 0))
    val exit = fadeOut(animationSpec = tween(if (animationsEnabled) duration / 2 else 0))

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = { enter },
        exitTransition = { exit },
        popEnterTransition = { enter },
        popExitTransition = { exit },
    ) {
        composable(Routes.HOME) {
            HomeRoute(
                onStartOrganizer = { navController.navigate(Routes.FILTERS) },
                onContinueOrganizer = { navController.navigate(Routes.ORGANIZER) },
                onOpenReview = { navController.navigate(Routes.REVIEW) },
                onOpenFavorites = { navController.navigate(Routes.FAVORITES) },
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onOpenInsights = { navController.navigate(Routes.INSIGHTS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.FILTERS) {
            FiltersRoute(
                onBack = { navController.popBackStack() },
                onSessionStarted = {
                    navController.navigate(Routes.ORGANIZER) {
                        popUpTo(Routes.HOME)
                    }
                },
            )
        }

        composable(Routes.ORGANIZER) {
            OrganizerRoute(
                onBack = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
                onOpenReview = { navController.navigate(Routes.REVIEW) },
                onOpenPhoto = { photoId -> navController.navigate(Routes.photoDetail(photoId)) },
                onChoosePhotos = { navController.navigate(Routes.FILTERS) },
            )
        }

        composable(Routes.REVIEW) {
            ReviewRoute(
                onBack = { navController.popBackStack() },
                onOpenPhoto = { photoId -> navController.navigate(Routes.photoDetail(photoId)) },
                onStartTidying = { navController.navigate(Routes.FILTERS) },
            )
        }

        composable(Routes.FAVORITES) {
            FavoritesRoute(
                onBack = { navController.popBackStack() },
                onOpenPhoto = { photoId -> navController.navigate(Routes.photoDetail(photoId)) },
                onSessionStarted = {
                    navController.navigate(Routes.ORGANIZER) {
                        popUpTo(Routes.HOME)
                    }
                },
            )
        }

        composable(Routes.SEARCH) {
            SearchRoute(
                onBack = { navController.popBackStack() },
                onOpenPhoto = { photoId -> navController.navigate(Routes.photoDetail(photoId)) },
            )
        }

        composable(Routes.INSIGHTS) {
            InsightsRoute(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsRoute(
                appVersion = BuildConfig.VERSION_NAME,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.PHOTO_DETAIL,
            arguments = listOf(
                navArgument("photoId") { type = NavType.StringType },
            ),
        ) {
            PhotoDetailRoute(onBack = { navController.popBackStack() })
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
