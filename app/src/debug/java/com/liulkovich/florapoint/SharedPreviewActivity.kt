
package com.liulkovich.florapoint

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.liulkovich.florapoint.presentation.screens.settings.SettingsViewModel
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.liulkovich.florapoint.presentation.screens.detail.DetailViewModel
import com.liulkovich.florapoint.presentation.screens.detail.toSharedDetailState
import com.liulkovich.florapoint.presentation.screens.guide.GuideViewModel
import com.liulkovich.florapoint.presentation.screens.guide.toAndroidCommand
import com.liulkovich.florapoint.presentation.screens.guide.toSharedGuideState
import com.liulkovich.florapoint.shared.ui.components.ForestPointBottomBar
import com.liulkovich.florapoint.shared.ui.detail.SharedDetailScreen
import com.liulkovich.florapoint.shared.ui.guide.SharedGuideScreen
import com.liulkovich.florapoint.shared.ui.theme.ForestPointSharedTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.appcompat.app.AppCompatDelegate
import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import com.liulkovich.florapoint.shared.ui.home.SharedHomeScreen
import com.liulkovich.florapoint.presentation.screens.home.HomeViewModel
import com.liulkovich.florapoint.presentation.screens.home.countDay
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import com.liulkovich.florapoint.presentation.screens.guide.GuideCommand
import com.liulkovich.florapoint.presentation.screens.settings.DownloadAreaScreen
import android.content.ContextWrapper
import android.content.res.Resources
import androidx.compose.runtime.key

@AndroidEntryPoint
class SharedPreviewActivity : ComponentActivity() {

    private val guideViewModel: GuideViewModel by viewModels()

    private val homeViewModel: HomeViewModel by viewModels()

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            val context = LocalContext.current

// Следим за языком из SettingsViewModel.
            val selectedLanguageTag by settingsViewModel
                .currentLanguage
                .collectAsStateWithLifecycle()

// Актуальная конфигурация устройства.
            val systemConfiguration = LocalConfiguration.current

            val systemLanguage =
                android.content.res.Resources.getSystem()
                    .configuration
                    .locales[0]
                    ?.language ?: "en"

// Язык, который должен использовать KMP-интерфейс.
            val language = when (selectedLanguageTag) {
                "ru" -> "ru"
                "en" -> "en"

                // Пустая строка означает системный язык.
                else -> if (systemLanguage == "ru") {
                    "ru"
                } else {
                    "en"
                }
            }

// Обновляется сразу после смены языка.

            val selectedLocale = remember(language) {
                Locale.forLanguageTag(language)
            }

// Compose Multiplatform использует текущую JVM-локаль
// при выборе перевода из composeResources.
// Это решение пока применяется только в debug-превью.
            Locale.setDefault(selectedLocale)

            val localizedConfiguration = remember(
                systemConfiguration,
                language
            ) {
                Configuration(systemConfiguration).apply {
                    setLocale(selectedLocale)
                }
            }

// Создаём ресурсы Android для выбранного языка,
// но сохраняем исходный Activity Context как базовый.
// Это важно для startActivity и других Android-действий.
            val localizedContext = remember(
                context,
                localizedConfiguration
            ) {
                val resourcesContext =
                    context.createConfigurationContext(
                        localizedConfiguration
                    )

                object : ContextWrapper(context) {
                    override fun getResources(): Resources =
                        resourcesContext.resources
                }
            }


            val darkTheme = isSystemInDarkTheme()

            val androidColorScheme =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (darkTheme) {
                        dynamicDarkColorScheme(context)
                    } else {
                        dynamicLightColorScheme(context)
                    }
                } else {
                    null
                }

            val navController = rememberNavController()

            val snackbarHostState = remember {
                SnackbarHostState()
            }
            val scope = rememberCoroutineScope()
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfiguration
            ) {
                key(language) {
                    ForestPointSharedTheme(
                        colorScheme = androidColorScheme
                    ) {
                NavHost(
                    navController = navController,
                    startDestination = "Home"
                ) {

                    composable("Home") {
                        val androidHomeState by homeViewModel.state.collectAsState()

                        val sharedHomeState = androidHomeState.toSharedHomeScreenState(
                            language = language
                        )

                        val notificationEnabledIds = androidHomeState.species
                            .filter { it.isNotifEnabled == 1 }
                            .map { it.id }
                            .toSet()

                        Scaffold(
                            snackbarHost = {
                                SnackbarHost(snackbarHostState)
                            },
                            bottomBar = {
                                ForestPointBottomBar(
                                    currentRoute = "Home",
                                    onNavigate = { route ->
                                        when (route) {
                                            "Guide" -> {
                                                navController.navigate("Guide") {
                                                    launchSingleTop = true
                                                }
                                            }

                                            "Home" -> Unit
                                            "Map" -> {
                                                navController.navigate("Map") {
                                                    launchSingleTop = true
                                                }
                                            }

                                            "Settings" -> {
                                                navController.navigate("Settings") {
                                                    launchSingleTop = true
                                                }
                                            }

                                            else -> {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        "Тест перехода: $route"
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onForecastClick = {
                                        navController.navigate("Forecast") {
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        ) { innerPadding ->


                            SharedHomeScreen(
                                state = sharedHomeState,
                                language = language,
                                notificationEnabledIds = notificationEnabledIds,
                                daysLeftCount = { endMonth ->
                                    countDay(endMonth)
                                },

                                onClickMap = {
                                    navController.navigate("Map") {
                                        launchSingleTop = true
                                    }
                                },

                                onClickCategory = { category ->
                                    val selectedCategories =
                                        guideViewModel.state.value.selectedCategories

                                    // Убираем другие выбранные категории
                                    selectedCategories
                                        .filter { it != category }
                                        .forEach { selected ->
                                            guideViewModel.processCommand(
                                                GuideCommand.CheckCategory(selected)
                                            )
                                        }

                                    // Включаем нужную категорию, если она не выбрана
                                    if (category !in selectedCategories) {
                                        guideViewModel.processCommand(
                                            GuideCommand.CheckCategory(category)
                                        )
                                    }

                                    // Сбрасываем старый поисковый запрос
                                    guideViewModel.processCommand(
                                        GuideCommand.InputSearchQuery("")
                                    )

                                    navController.navigate("Guide") {
                                        launchSingleTop = true
                                    }
                                },

                                onSeasonCategorySelected = { category ->
                                    homeViewModel.onSeasonCategorySelected(category)
                                },

                                onNotificationChange = { speciesId, enabled ->
                                    homeViewModel.toggleNotification(
                                        speciesId,
                                        enabled
                                    )
                                },

                                onClickDetail = { speciesId ->
                                    navController.navigate(
                                        "Detail/$speciesId"
                                    )
                                },

                                mapContent = { mapModifier ->
                                    AndroidView(
                                        factory = { mapContext ->
                                            MapView(mapContext).apply {
                                                setTileSource(
                                                    TileSourceFactory.MAPNIK
                                                )

                                                setMultiTouchControls(false)

                                                controller.setZoom(12.0)

                                                controller.setCenter(
                                                    GeoPoint(
                                                        53.133562,
                                                        25.141006
                                                    )
                                                )
                                            }
                                        },
                                        modifier = mapModifier
                                    )
                                },

                                categoryImageContent = { imageKey, imageModifier ->
                                    SharedHomeCategoryImage(
                                        imageKey = imageKey,
                                        modifier = imageModifier
                                    )
                                },

                                seasonImageContent = { imageName, imageModifier ->
                                    val drawableId =
                                        context.resources.getIdentifier(
                                            imageName,
                                            "drawable",
                                            context.packageName
                                        ).takeIf { it != 0 }
                                            ?: R.drawable.ic_launcher_background

                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(drawableId)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = imageModifier
                                    )
                                },

                                contentPadding = innerPadding
                            )

                        }
                    }


                    composable("Guide") {
                        val androidState by guideViewModel.state.collectAsState()
                        val sharedState = androidState.toSharedGuideState()

                        Scaffold(
                            snackbarHost = {
                                SnackbarHost(snackbarHostState)
                            },
                            bottomBar = {
                                ForestPointBottomBar(
                                    currentRoute = "Guide",

                                    onNavigate = { route ->
                                        when (route) {
                                            "Home" -> {
                                                val returned = navController.popBackStack(
                                                    route = "Home",
                                                    inclusive = false
                                                )

                                                if (!returned) {
                                                    navController.navigate("Home") {
                                                        launchSingleTop = true
                                                    }
                                                }
                                            }

                                            "Guide" -> Unit

                                            "Map" -> {
                                                navController.navigate("Map") {
                                                    launchSingleTop = true
                                                }
                                            }

                                            "Settings" -> {
                                                navController.navigate("Settings") {
                                                    launchSingleTop = true
                                                }
                                            }

                                            else -> {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        "Тест перехода: $route"
                                                    )
                                                }
                                            }
                                        }
                                    },

                                    onForecastClick = {
                                        navController.navigate("Forecast") {
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier.padding(innerPadding)
                            ) {
                                SharedGuideScreen(
                                    state = sharedState,
                                    language = language,
                                    onCommand = { command ->
                                        guideViewModel.processCommand(
                                            command.toAndroidCommand()
                                        )
                                    },
                                    onClickDetail = { speciesId ->
                                        navController.navigate(
                                            "Detail/$speciesId"
                                        )
                                    },
                                    imageContent = { imageName, imageModifier ->
                                        val firstImageName = imageName
                                            .split(",")
                                            .map { it.trim() }
                                            .firstOrNull()
                                            ?: ""

                                        val drawableId =
                                            context.resources.getIdentifier(
                                                firstImageName,
                                                "drawable",
                                                context.packageName
                                            ).takeIf { it != 0 }
                                                ?: R.drawable.ic_launcher_background

                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(drawableId)
                                                .crossfade(true)
                                                .size(144)
                                                .build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = imageModifier.clip(
                                                RoundedCornerShape(12.dp)
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }


                    composable("Map") {
                        Scaffold(
                            snackbarHost = {
                                SnackbarHost(snackbarHostState)
                            },
                            bottomBar = {
                                ForestPointBottomBar(
                                    currentRoute = "Map",

                                    onNavigate = { route ->
                                        when (route) {
                                            "Home" -> {
                                                val returned = navController.popBackStack(
                                                    route = "Home",
                                                    inclusive = false
                                                )

                                                if (!returned) {
                                                    navController.navigate("Home") {
                                                        launchSingleTop = true
                                                    }
                                                }
                                            }

                                            "Guide" -> {
                                                navController.navigate("Guide") {
                                                    launchSingleTop = true
                                                }
                                            }

                                            "Map" -> Unit
                                            "Settings" -> {
                                                navController.navigate("Settings") {
                                                    launchSingleTop = true
                                                }
                                            }

                                            else -> {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        "Тест перехода: $route"
                                                    )
                                                }
                                            }
                                        }
                                    },

                                    onForecastClick = {
                                        navController.navigate("Forecast") {
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier.padding(innerPadding)
                            ) {
                                SharedMapWithPointsPreview()
                            }
                        }
                    }

                    composable("Forecast") {
                        Scaffold(
                            snackbarHost = {
                                SnackbarHost(snackbarHostState)
                            },
                            bottomBar = {
                                ForestPointBottomBar(
                                    currentRoute = "Forecast",
                                    onNavigate = { route ->
                                        when (route) {
                                            "Home" -> {
                                                val returned = navController.popBackStack(
                                                    route = "Home",
                                                    inclusive = false
                                                )

                                                if (!returned) {
                                                    navController.navigate("Home") {
                                                        launchSingleTop = true
                                                    }
                                                }
                                            }

                                            "Guide" -> {
                                                navController.navigate("Guide") {
                                                    launchSingleTop = true
                                                }
                                            }

                                            "Forecast" -> Unit

                                            "Map" -> {
                                                navController.navigate("Map") {
                                                    launchSingleTop = true
                                                }
                                            }

                                            "Settings" -> {
                                                navController.navigate("Settings") {
                                                    launchSingleTop = true
                                                }
                                            }

                                            else -> {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        "Тест перехода: $route"
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onForecastClick = {
                                        // Уже на экране прогноза
                                    }
                                )
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier.padding(innerPadding)
                            ) {
                                SharedForecastPreview(
                                    language = language,
                                    onClickDetail = { speciesId ->
                                        navController.navigate(
                                            "Detail/$speciesId"
                                        )
                                    },
                                    onLocationClick = { pointId ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "Тест перехода к точке: $pointId"
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }


                    composable(
                        route = "Detail/{speciesId}",
                        arguments = listOf(
                            navArgument("speciesId") {
                                type = NavType.IntType
                            }
                        )
                    ) {
                        val detailViewModel: DetailViewModel =
                            hiltViewModel()

                        val detailState by
                        detailViewModel.state.collectAsState()

                        SharedDetailScreen(
                            state = detailState.toSharedDetailState(),
                            language = language,
                            onBack = {
                                navController.popBackStack()
                            },
                            onNotificationToggle = { enabled ->
                                detailViewModel.toggleNotification(enabled)
                            },
                            onSpeciesClick = { speciesId ->
                                navController.navigate(
                                    "Detail/$speciesId"
                                )
                            },
                            imageContent = { imageName, imageModifier ->
                                val drawableId =
                                    context.resources.getIdentifier(
                                        imageName,
                                        "drawable",
                                        context.packageName
                                    ).takeIf { it != 0 }
                                        ?: R.drawable.ic_launcher_background

                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(drawableId)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = imageModifier
                                )
                            }
                        )
                    }


                    composable("Settings") {
                        Scaffold(
                            bottomBar = {
                                ForestPointBottomBar(
                                    currentRoute = "Settings",
                                    onNavigate = { route ->
                                        if (route != "Settings") {
                                            navController.navigate(route) {
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                    onForecastClick = {
                                        navController.navigate("Forecast") {
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        ) { innerPadding ->
                            SharedSettingsPreview(
                                modifier = Modifier.padding(innerPadding),

                                viewModel = settingsViewModel,

                                onNavigateToNotifications = {
                                    navController.navigate("Notifications")
                                },

                                onNavigateToOfflineRegions = {
                                    navController.navigate("OfflineRegions")
                                }
                            )
                        }
                    }


                    composable("Notifications") {
                        Scaffold(
                            bottomBar = {
                                ForestPointBottomBar(
                                    currentRoute = "Settings",
                                    onNavigate = { route ->
                                        if (route == "Settings") {
                                            navController.popBackStack(
                                                "Settings",
                                                false
                                            )
                                        } else {
                                            navController.navigate(route) {
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                    onForecastClick = {
                                        navController.navigate("Forecast") {
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier.padding(innerPadding)
                            ) {
                                SharedNotificationPreview()
                            }
                        }
                    }

                    composable("OfflineRegions") {
                        Scaffold(
                            bottomBar = {
                                ForestPointBottomBar(
                                    currentRoute = "Settings",
                                    onNavigate = { route ->
                                        if (route == "Settings") {
                                            navController.popBackStack(
                                                "Settings",
                                                false
                                            )
                                        } else {
                                            navController.navigate(route) {
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                    onForecastClick = {
                                        navController.navigate("Forecast") {
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier.padding(innerPadding)
                            ) {
                                SharedOfflineRegionsPreview(
                                    viewModel = settingsViewModel,

                                    onNavigateToDownload = {
                                        navController.navigate("DownloadArea")
                                    }
                                )
                            }
                        }
                    }

                    composable("DownloadArea") {
                        Scaffold(
                            bottomBar = {
                                ForestPointBottomBar(
                                    currentRoute = "Settings",
                                    onNavigate = { route ->
                                        if (route == "Settings") {
                                            navController.popBackStack(
                                                "Settings",
                                                false
                                            )
                                        } else {
                                            navController.navigate(route) {
                                                launchSingleTop = true
                                            }
                                        }
                                    },
                                    onForecastClick = {
                                        navController.navigate("Forecast") {
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier.padding(innerPadding)
                            ) {
                                SharedDownloadAreaPreview(
                                    viewModel = settingsViewModel
                                )
                            }
                        }
                    }
                }

                }
            }

        }
        }
    }
}
