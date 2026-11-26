package com.liulkovich.florapoint.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.background
import com.liulkovich.florapoint.shared.ui.map.SharedMapWeatherBanner
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.liulkovich.florapoint.shared.generated.resources.map_action_failed
import com.liulkovich.florapoint.shared.generated.resources.map_ok
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.key
import com.liulkovich.florapoint.shared.domain.SharedMapProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liulkovich.florapoint.shared.ui.map.SharedMapNavigationControls
import com.liulkovich.florapoint.shared.generated.resources.osm_attribution
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.ComposeUIViewController
import com.liulkovich.florapoint.shared.domain.ReferenceModel
import com.liulkovich.florapoint.shared.domain.SharedDetailScreenState
import com.liulkovich.florapoint.shared.domain.SharedFloraCategory
import com.liulkovich.florapoint.shared.domain.SharedForecastScreenState
import com.liulkovich.florapoint.shared.domain.SharedGuideCommand
import com.liulkovich.florapoint.shared.domain.SharedGuideScreenState
import com.liulkovich.florapoint.shared.domain.SharedHomeScreenState
import com.liulkovich.florapoint.shared.domain.SharedSocialForecastGateway
import com.liulkovich.florapoint.shared.domain.SharedWeatherData
import com.liulkovich.florapoint.shared.domain.calculateSharedForecast
import com.liulkovich.florapoint.shared.domain.countSeasonDaysLeft
import com.liulkovich.florapoint.shared.domain.loadSharedWeather
import com.liulkovich.florapoint.shared.generated.resources.Res
import com.liulkovich.florapoint.shared.generated.resources.map
import com.liulkovich.florapoint.shared.generated.resources.settings_screen_title
import com.liulkovich.florapoint.shared.generated.resources.forecast_title
import com.liulkovich.florapoint.shared.generated.resources.berry
import com.liulkovich.florapoint.shared.generated.resources.mushroom
import com.liulkovich.florapoint.shared.generated.resources.nut
import com.liulkovich.florapoint.shared.generated.resources.plant
import com.liulkovich.florapoint.shared.ui.components.ForestPointBottomBar
import com.liulkovich.florapoint.shared.ui.components.SharedSpeciesImage
import com.liulkovich.florapoint.shared.ui.detail.SharedDetailScreen
import com.liulkovich.florapoint.shared.ui.forecast.SharedForecastScreen
import com.liulkovich.florapoint.shared.ui.guide.SharedGuideScreen
import com.liulkovich.florapoint.shared.ui.home.SharedHomeScreen
import com.liulkovich.florapoint.shared.ui.theme.ForestPointSharedTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.painterResource
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.UIKit.UIViewController
import kotlinx.coroutines.async
import com.liulkovich.florapoint.shared.domain.SharedSocialForecastSessionCache
import com.liulkovich.florapoint.shared.domain.SharedSocialForecastError
import com.liulkovich.florapoint.shared.domain.SharedMapPoint
import com.liulkovich.florapoint.shared.domain.localizedName
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import com.liulkovich.florapoint.shared.domain.SharedUserPoint
import com.liulkovich.florapoint.shared.ui.map.SharedAddMapPointSheet
import com.liulkovich.florapoint.shared.ui.map.SharedMapActionButtons
import kotlin.time.Clock
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import com.liulkovich.florapoint.shared.domain.SharedMapPointsState
import com.liulkovich.florapoint.shared.ui.map.SharedMapPointsList
import platform.Foundation.NSDateFormatter


import androidx.compose.foundation.layout.Arrangement
import com.liulkovich.florapoint.shared.ui.map.SharedMapLayerMenu
import com.liulkovich.florapoint.shared.domain.SharedMapSettings

@OptIn(ExperimentalMaterial3Api::class)
fun MainViewController(
    socialGateway: SharedSocialForecastGateway,
    mapBridge: IosMapBridge,
    googleMapBridge: IosGoogleMapBridge,
    googleMapsEnabled: Boolean
): UIViewController =
    ComposeUIViewController {

        var currentRoute by remember {
            mutableStateOf("Home")
        }

        val language = remember {
            currentIosLanguage()
        }

        var weather by remember {
            mutableStateOf<SharedWeatherData?>(null)
        }

        var forecastState by remember {
            mutableStateOf(SharedForecastScreenState())
        }

        var mapSettings by remember {
            mutableStateOf(SharedMapSettings())
        }

        val socialForecastCache = remember {
            SharedSocialForecastSessionCache()
        }

        var weatherLoading by remember {
            mutableStateOf(false)
        }

        var allFloraSpecies by remember {
            mutableStateOf(loadFloraSpecies())
        }

        val changeFloraNotification: (Int, Boolean) -> Unit = { speciesId, enabled ->
            // Сохраняем значение в SQLite.
            updateFloraNotification(
                speciesId = speciesId,
                enabled = enabled
            )

            // Обновляем данные для всех экранов.
            allFloraSpecies = allFloraSpecies.map { species ->
                if (species.id == speciesId) {
                    species.copy(
                        isNotifEnabled = if (enabled) 1 else 0
                    )
                } else {
                    species
                }
            }
        }


        var selectedSeasonCategory by remember {
            mutableStateOf<String?>(null)
        }

        var guideQuery by remember {
            mutableStateOf("")
        }

        var selectedDetailSpeciesId by remember {
            mutableStateOf<Int?>(null)
        }

        var detailReturnRoute by remember {
            mutableStateOf("Guide")
        }


        var guideSelectedCategories by remember {
            mutableStateOf<Set<String>>(emptySet())
        }

        val tipText = remember(language) {
            loadRandomFloraTip(language = language)
        }

        LaunchedEffect(currentRoute, language) {

            if (currentRoute != "Forecast") {
                return@LaunchedEffect
            }

            forecastState = SharedForecastScreenState(
                isLoading = true
            )

            try {

                val location = withTimeoutOrNull(30_000L) {
                    requestIosCoordinates()
                }

                if (location == null) {
                    println("Forecast: координаты недоступны")

                    forecastState = SharedForecastScreenState(
                        isLoading = false,
                        noWeather = true
                    )

                    return@LaunchedEffect
                }

                println(
                    "Forecast: координаты получены"
                )

                val weatherData = loadSharedWeather(
                    latitude = location.latitude,
                    longitude = location.longitude
                )

                if (weatherData == null) {
                    forecastState = SharedForecastScreenState(
                        isLoading = false,
                        noWeather = true
                    )
                    return@LaunchedEffect
                }

                val cachedSocial = socialForecastCache.get(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    language = language
                )

                val socialDeferred = if (cachedSocial == null) {
                    async {
                        requestIosSocialForecast(
                            gateway = socialGateway,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            language = language
                        )
                    }
                } else {
                    null
                }

                val conditions = loadFloraConditions()

                val currentMonth = NSCalendar.currentCalendar
                    .component(
                        NSCalendarUnitMonth,
                        fromDate = NSDate()
                    )
                    .toInt()

                val forecastItems = calculateSharedForecast(
                    species = allFloraSpecies,
                    conditions = conditions,
                    weather = weatherData,
                    currentMonth = currentMonth
                )


                forecastState = SharedForecastScreenState(
                    isLoading = false,
                    noWeather = false,
                    items = forecastItems,

                    temperature = weatherData.temperature,
                    humidity = weatherData.humidity,
                    avgTemp = weatherData.avgTemp5Days,
                    avgHumidity = weatherData.avgHumidity5Days,
                    currentMonth = currentMonth,

                    socialLoading = socialDeferred != null,
                    socialMessage = cachedSocial?.message,
                    socialSpecies = cachedSocial?.species ?: emptyList(),
                    socialError = null
                )


                if (socialDeferred != null) {

                    when (val result = socialDeferred.await()) {

                        is IosSocialForecastResult.Success -> {

                            socialForecastCache.save(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                language = language,
                                message = result.message,
                                species = result.species
                            )

                            println(
                                "SocialForecast: получен ответ Firebase, " +
                                        "публикаций: ${result.postsAnalyzed}"
                            )

                            forecastState = forecastState.copy(
                                socialLoading = false,
                                socialMessage = result.message,
                                socialSpecies = result.species,
                                socialError = null
                            )
                        }

                        is IosSocialForecastResult.Failure -> {

                            println(
                                "SocialForecast: ошибка Firebase, " +
                                        "noInternet=${result.noInternet}"
                            )

                            forecastState = forecastState.copy(
                                socialLoading = false,
                                socialMessage = null,
                                socialSpecies = emptyList(),
                                socialError = if (result.noInternet) {
                                    SharedSocialForecastError.NO_INTERNET
                                } else {
                                    SharedSocialForecastError.SERVICE_UNAVAILABLE
                                }
                            )
                        }
                    }
                }



            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("Forecast loading failed: $e")

                forecastState = SharedForecastScreenState(
                    isLoading = false,
                    noWeather = true
                )
            }
        }

        ForestPointSharedTheme(
            darkTheme = true
        ) {


            Scaffold(
                bottomBar = {
                    if (currentRoute != "Detail") {
                        ForestPointBottomBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                currentRoute = route
                            },
                            onForecastClick = {
                                currentRoute = "Forecast"
                            },
                            compact = false
                        )
                    }
                }
            ) { innerPadding ->

                when (currentRoute) {

                    "Home" -> {
                        SharedHomeScreen(
                            state = SharedHomeScreenState(

                                species = remember(selectedSeasonCategory, allFloraSpecies) {
                                    val currentMonth = NSCalendar.currentCalendar.component(
                                        NSCalendarUnitMonth,
                                        fromDate = NSDate()
                                    ).toInt()

                                    allFloraSpecies
                                        .filter { item ->
                                            val inSeason =
                                                if (item.startMonth <= item.endMonth) {
                                                    currentMonth in item.startMonth..item.endMonth
                                                } else {
                                                    currentMonth >= item.startMonth ||
                                                            currentMonth <= item.endMonth
                                                }

                                            item.category != "other" &&
                                                    item.isReferenceOnly != 1 &&
                                                    (selectedSeasonCategory == null ||
                                                            item.category == selectedSeasonCategory) &&
                                                    inSeason
                                        }
                                        .sortedWith(
                                            compareBy<ReferenceModel> {
                                                (it.endMonth.coerceIn(1, 12) -
                                                        currentMonth + 12) % 12
                                            }.thenBy { it.id }
                                        )
                                },

                                tipText = tipText,
                                isLoading = false,
                                isTipLoading = false,
                                selectedSeasonCategory = selectedSeasonCategory
                            ),

                            language = language,


                            notificationEnabledIds = allFloraSpecies
                                .filter { it.isNotifEnabled == 1 }
                                .map { it.id }
                                .toSet(),


                            daysLeftCount = { endMonth ->
                                countDayIos(endMonth)
                            },

                            onClickMap = {
                                currentRoute = "Map"
                            },

                            onClickCategory = { category ->
                                guideQuery = ""
                                guideSelectedCategories = setOf(category)
                                currentRoute = "Guide"
                            },

                            onSeasonCategorySelected = { category ->
                                selectedSeasonCategory = category
                            },

                            onNotificationChange = { speciesId, enabled ->
                                changeFloraNotification(speciesId, enabled)
                            },


                            onClickDetail = { speciesId ->
                                detailReturnRoute = "Home"
                                selectedDetailSpeciesId = speciesId
                                currentRoute = "Detail"
                            },


                            mapContent = { modifier ->
                                // Пока без карты.
                                // Надпись "Открыть карту" уже
                                // рисует SharedHomeMapPanel.
                                Box(modifier = modifier)
                            },


                            categoryImageContent = { categoryKey, modifier ->

                                val imageResource = when (categoryKey) {
                                    "mushroom" -> Res.drawable.mushroom
                                    "berry" -> Res.drawable.berry
                                    "plant" -> Res.drawable.plant
                                    "nut" -> Res.drawable.nut
                                    else -> null
                                }

                                if (imageResource != null) {
                                    Image(
                                        painter = painterResource(imageResource),
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = modifier
                                    )
                                } else {
                                    Box(
                                        modifier = modifier,
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = SharedFloraCategory
                                                .fromKey(categoryKey)
                                                ?.emoji ?: "🌿"
                                        )
                                    }
                                }
                            },



                            seasonImageContent = { imageName, modifier ->
                                SharedSpeciesImage(
                                    imageName = imageName,
                                    modifier = modifier
                                )
                            },


                            contentPadding = innerPadding
                        )
                    }


                    "Guide" -> {

                        // Загружаем все виды из настоящей SQLite-базы.
                        val allGuideSpecies = allFloraSpecies

                        // Применяем поиск и выбранные категории.
                        val filteredGuideSpecies = remember(
                            allGuideSpecies,
                            guideQuery,
                            guideSelectedCategories
                        ) {
                            val query = guideQuery.trim()

                            allGuideSpecies.filter { species ->

                                val matchesCategory =
                                    guideSelectedCategories.isEmpty() ||
                                            species.category in guideSelectedCategories

                                val matchesQuery =
                                    query.isEmpty() ||
                                            species.nameRu.contains(
                                                query,
                                                ignoreCase = true
                                            ) ||
                                            species.nameEn?.contains(
                                                query,
                                                ignoreCase = true
                                            ) == true

                                matchesCategory && matchesQuery
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            SharedGuideScreen(
                                state = SharedGuideScreenState(
                                    query = guideQuery,
                                    species = filteredGuideSpecies,
                                    selectedCategories = guideSelectedCategories,
                                    isLoading = false
                                ),

                                language = language,

                                onCommand = { command ->
                                    when (command) {

                                        is SharedGuideCommand.InputSearchQuery -> {
                                            guideQuery = command.query
                                        }

                                        is SharedGuideCommand.CheckCategory -> {
                                            val category = command.check

                                            guideSelectedCategories =
                                                if (category in guideSelectedCategories) {
                                                    guideSelectedCategories - category
                                                } else {
                                                    guideSelectedCategories + category
                                                }
                                        }

                                        is SharedGuideCommand.ToggleNotification -> {
                                            changeFloraNotification(
                                                command.id,
                                                command.enabled == 1
                                            )
                                        }
                                    }
                                },


                                onClickDetail = { speciesId ->
                                    detailReturnRoute = "Guide"
                                    selectedDetailSpeciesId = speciesId
                                    currentRoute = "Detail"
                                },


                                imageContent = { imageName, modifier ->
                                    SharedSpeciesImage(
                                        imageName = imageName,
                                        modifier = modifier
                                    )
                                }
                            )
                        }
                    }


                    "Detail" -> {

                        val detailSpecies = allFloraSpecies.firstOrNull { species ->
                            species.id == selectedDetailSpeciesId
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            SharedDetailScreen(
                                state = SharedDetailScreenState(
                                    species = detailSpecies,
                                    isNotificationEnabled =
                                        detailSpecies?.isNotifEnabled == 1,
                                    resolvedLookAlikes = emptyMap()
                                ),

                                language = language,


                                onBack = {
                                    selectedDetailSpeciesId = null
                                    currentRoute = detailReturnRoute
                                },


                                onNotificationToggle = { enabled ->
                                    selectedDetailSpeciesId?.let { speciesId ->
                                        changeFloraNotification(speciesId, enabled)
                                    }
                                },

                                onSpeciesClick = { speciesId ->
                                    selectedDetailSpeciesId = speciesId
                                },

                                imageContent = { imageName, modifier ->
                                    SharedSpeciesImage(
                                        imageName = imageName,
                                        modifier = modifier
                                    )
                                }
                            )
                        }
                    }


                    "Map" -> {
                        val mapScope = rememberCoroutineScope()

                        val activeMapBridge: IosMapBridge =
                            if (
                                googleMapsEnabled &&
                                mapSettings.provider == SharedMapProvider.GOOGLE
                            ) {
                                googleMapBridge
                            } else {
                                mapBridge
                            }

                        var mapWeather by remember {
                            mutableStateOf<SharedWeatherData?>(null)
                        }

                        val weatherLoadingPointIds = remember {
                            mutableStateMapOf<Int, Boolean>()
                        }

                        var pointsRevision by remember {
                            mutableIntStateOf(0)
                        }

                        var pendingAddLocation by remember {
                            mutableStateOf<Pair<Double, Double>?>(null)
                        }

                        var saveFailed by remember {
                            mutableStateOf(false)
                        }

                        var mapSearchQuery by remember {
                            mutableStateOf("")
                        }

                        var selectedMapPointKey by remember {
                            mutableStateOf<String?>(null)
                        }

                        var editingPoint by remember {
                            mutableStateOf<SharedUserPoint?>(null)
                        }

                        var editSaveFailed by remember {
                            mutableStateOf(false)
                        }

                        var mapActionFailed by remember {
                            mutableStateOf(false)
                        }

                        DisposableEffect(activeMapBridge) {
                            val listener = object : IosMapTapListener {
                                override fun onMapTap(
                                    latitude: Double,
                                    longitude: Double
                                ) {
                                    saveFailed = false
                                    pendingAddLocation = latitude to longitude
                                }
                            }

                            activeMapBridge.setMapTapListener(listener)

                            onDispose {
                                activeMapBridge.setMapTapListener(null)
                            }
                        }

                        val mapPoints = remember(
                            allFloraSpecies,
                            language,
                            pointsRevision
                        ) {

                            val speciesById = allFloraSpecies.associateBy { it.id }

                            loadIosUserPoints().map { point ->

                                val reference = point.speciesId?.let { speciesId ->
                                    speciesById[speciesId]
                                }

                                val displayName = reference
                                    ?.localizedName(language)
                                    ?.takeIf { it.isNotBlank() }
                                    ?: point.userName

                                SharedMapPoint(
                                    key = "local_${point.id}",
                                    id = point.id,
                                    speciesId = point.speciesId,

                                    displayName = displayName,
                                    userName = point.userName,
                                    description = point.description,

                                    category = point.category
                                        ?.takeIf { it.isNotBlank() }
                                        ?: reference?.category
                                        ?: "custom",

                                    latitude = point.latitude,
                                    longitude = point.longitude,
                                    timestampSeconds = point.timestamp,

                                    isOwner = true,
                                    isPublic = point.isPublic,

                                    temperature = point.temperature,
                                    humidity = point.humidity,
                                    avgTemp5Days = point.avgTemp5Days,
                                    avgHumidity5Days = point.avgHumidity5Days
                                )
                            }
                        }

                        LaunchedEffect(activeMapBridge, mapPoints) {
                            activeMapBridge.updatePoints(mapPoints)
                        }



                        LaunchedEffect(activeMapBridge) {
                            val location = withTimeoutOrNull(30_000L) {
                                requestIosCoordinates()
                            }

                            location?.let {
                                activeMapBridge.centerOn(
                                    latitude = it.latitude,
                                    longitude = it.longitude
                                )

                                mapWeather = withTimeoutOrNull(20_000L) {
                                    loadSharedWeather(
                                        latitude = it.latitude,
                                        longitude = it.longitude
                                    )
                                }
                            }
                        }



                        val dateFormatter = remember {
                            NSDateFormatter().apply {
                                dateFormat = "dd.MM.yyyy"
                            }
                        }

                        val listPoints = mapPoints.map { point ->
                            point.copy(
                                isSelected = point.key == selectedMapPointKey,
                                isWeatherLoading = weatherLoadingPointIds[point.id] == true
                            )
                        }

                        val mapPanel: @Composable (Modifier, Boolean) -> Unit =
                            { panelModifier, isLandscape ->

                                Box(modifier = panelModifier) {

                                    key(mapSettings.provider) {
                                        IosOsmMapContent(
                                            mapBridge = activeMapBridge,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(8.dp)
                                        )
                                    }

                                    mapWeather?.let { currentWeather ->
                                        SharedMapWeatherBanner(
                                            weather = currentWeather,
                                            modifier = Modifier
                                                .align(Alignment.TopCenter)
                                                .padding(
                                                    top = 8.dp,
                                                    start = 8.dp,
                                                    end = 8.dp
                                                )
                                        )
                                    }

                                    SharedMapLayerMenu(
                                        settings = mapSettings,
                                        onProviderSelected = { provider ->
                                            mapSettings = mapSettings.copy(
                                                provider = provider
                                            )
                                            if (provider == SharedMapProvider.GOOGLE) {
                                                googleMapBridge.setGoogleLayer(
                                                    mapSettings.googleLayer.name
                                                )
                                            }
                                        },
                                        onGoogleLayerSelected = { layer ->
                                            mapSettings = mapSettings.copy(
                                                googleLayer = layer
                                            )
                                            googleMapBridge.setGoogleLayer(layer.name)
                                        },
                                        showGoogleProvider = googleMapsEnabled,

                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(
                                                top = if (isLandscape) 8.dp else 72.dp,
                                                end = 16.dp
                                            ),
                                        isLandscape = isLandscape

                                    )


                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(
                                                end = 16.dp,
                                                bottom = 16.dp
                                            ),
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SharedMapNavigationControls(
                                            onMyLocation = {
                                                mapScope.launch {
                                                    val location = withTimeoutOrNull(30_000L) {
                                                        requestIosCoordinates()
                                                    }

                                                    location?.let {
                                                        activeMapBridge.centerOn(
                                                            latitude = it.latitude,
                                                            longitude = it.longitude
                                                        )
                                                    }
                                                }
                                            },
                                            onZoomIn = {
                                                activeMapBridge.zoomIn()
                                            },
                                            onZoomOut = {
                                                activeMapBridge.zoomOut()
                                            },
                                            isLandscape = isLandscape
                                        )

                                        SharedMapActionButtons(
                                            isAuthorized = false,
                                            showOnlyMyPoints = true,
                                            onAddPoint = {
                                                mapScope.launch {
                                                    val location = withTimeoutOrNull(30_000L) {
                                                        requestIosCoordinates()
                                                    }

                                                    location?.let {
                                                        saveFailed = false
                                                        pendingAddLocation =
                                                            it.latitude to it.longitude
                                                    }
                                                }
                                            },
                                            onToggleFilter = {},
                                            isLandscape = isLandscape
                                        )
                                    }


                                    if (
                                        mapSettings.provider == SharedMapProvider.OSM ||
                                        !googleMapsEnabled
                                    ) {
                                        Text(
                                            text = stringResource(
                                                Res.string.osm_attribution
                                            ),
                                            fontSize = 10.sp,
                                            color = Color.Black,
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(12.dp)
                                                .background(
                                                    Color.White.copy(alpha = 0.8f)
                                                )
                                                .padding(
                                                    horizontal = 5.dp,
                                                    vertical = 3.dp
                                                )
                                        )
                                    }
                                }
                            }

                        val pointsPanel: @Composable (Modifier) -> Unit =
                            { panelModifier ->

                                SharedMapPointsList(
                                    state = SharedMapPointsState(
                                        points = listPoints,
                                        searchQuery = mapSearchQuery,
                                        selectedPointKey = selectedMapPointKey
                                    ),
                                    onSearchQueryChange = { query ->
                                        mapSearchQuery = query
                                    },
                                    onPointClick = { point ->
                                        selectedMapPointKey = point.key

                                        activeMapBridge.centerOn(
                                            latitude = point.latitude,
                                            longitude = point.longitude
                                        )
                                    },
                                    onPointLongClick = { point ->
                                        selectedMapPointKey = point.key

                                        activeMapBridge.centerOn(
                                            latitude = point.latitude,
                                            longitude = point.longitude
                                        )
                                    },


                                    onEdit = { point ->
                                        try {
                                            val original = loadIosUserPoints()
                                                .firstOrNull { it.id == point.id }

                                            if (original != null) {
                                                editSaveFailed = false
                                                editingPoint = original
                                            } else {
                                                mapActionFailed = true
                                            }
                                        } catch (e: Exception) {
                                            println("Map edit open failed: $e")
                                            mapActionFailed = true
                                        }
                                    },

                                    onDelete = { point ->
                                        try {
                                            val deleted = deleteIosUserPoint(point.id)

                                            if (deleted) {
                                                if (selectedMapPointKey == point.key) {
                                                    selectedMapPointKey = null
                                                }

                                                pointsRevision++
                                            } else {
                                                mapActionFailed = true
                                            }
                                        } catch (e: Exception) {
                                            println("Map delete failed: $e")
                                            mapActionFailed = true
                                        }
                                    },

                                    onShare = { point ->
                                        activeMapBridge.sharePoint(
                                            latitude = point.latitude,
                                            longitude = point.longitude,
                                            title = point.displayName
                                        )
                                    },

                                    showActions = true,


                                    formatDate = { seconds ->
                                        dateFormatter.stringFromDate(
                                            NSDate(
                                                timeIntervalSinceReferenceDate =
                                                    seconds.toDouble() - 978_307_200.0
                                            )
                                        )
                                    },
                                    formatDistance = { distance ->
                                        distance.toString()
                                    },

                                    modifier = panelModifier
                                )
                            }

                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            val isLandscape = maxWidth > maxHeight

                            if (isLandscape) {
                                Row(Modifier.fillMaxSize()) {

                                    mapPanel(
                                        Modifier
                                            .weight(2f)
                                            .fillMaxHeight(),
                                        true
                                    )

                                    VerticalDivider()

                                    pointsPanel(
                                        Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                }
                            } else {
                                Column(Modifier.fillMaxSize()) {

                                    mapPanel(
                                        Modifier
                                            .weight(2f)
                                            .fillMaxWidth(),
                                        false
                                    )

                                    HorizontalDivider()

                                    pointsPanel(
                                        Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    )
                                }
                            }
                        }
                        pendingAddLocation?.let { (latitude, longitude) ->

                            ModalBottomSheet(
                                onDismissRequest = {
                                    pendingAddLocation = null
                                }
                            ) {
                                SharedAddMapPointSheet(
                                    species = allFloraSpecies,
                                    language = language,
                                    saveFailed = saveFailed,


                                    onSave = { speciesId, name, description, category ->
                                        try {
                                            val savedId = insertIosUserPoint(
                                                SharedUserPoint(
                                                    speciesId = speciesId,
                                                    latitude = latitude,
                                                    longitude = longitude,
                                                    userName = name,
                                                    description = description,
                                                    timestamp = Clock.System.now().epochSeconds,
                                                    category = category
                                                )
                                            ).toInt()

                                            pointsRevision++
                                            pendingAddLocation = null
                                            saveFailed = false

                                            weatherLoadingPointIds[savedId] = true

                                            mapScope.launch {
                                                try {
                                                    val weather = withTimeoutOrNull(25_000L) {
                                                        loadSharedWeather(
                                                            latitude = latitude,
                                                            longitude = longitude
                                                        )
                                                    }

                                                    if (weather != null) {
                                                        val updated = withContext(Dispatchers.Default) {
                                                            updateIosUserPointWeather(
                                                                id = savedId,
                                                                weather = weather,
                                                                timestampMillis =
                                                                    Clock.System.now().epochSeconds * 1000L
                                                            )
                                                        }

                                                        if (updated) {
                                                            pointsRevision++
                                                        }
                                                    }

                                                } catch (e: CancellationException) {
                                                    throw e

                                                } catch (e: Exception) {
                                                    println("Map point weather failed: $e")

                                                } finally {
                                                    weatherLoadingPointIds.remove(savedId)
                                                }
                                            }

                                        } catch (e: Exception) {
                                            println("Map point save failed: $e")
                                            saveFailed = true
                                        }
                                    },


                                    onDismiss = {
                                        pendingAddLocation = null
                                    }
                                )
                            }
                        }


                        editingPoint?.let { original ->

                            ModalBottomSheet(
                                onDismissRequest = {
                                    editingPoint = null
                                }
                            ) {
                                SharedAddMapPointSheet(
                                    species = allFloraSpecies,
                                    language = language,
                                    initialPoint = original,
                                    saveFailed = editSaveFailed,

                                    onSave = {
                                            speciesId,
                                            name,
                                            description,
                                            category ->

                                        try {
                                            val updated = updateIosUserPoint(
                                                original.copy(
                                                    speciesId = speciesId,
                                                    userName = name,
                                                    description = description,
                                                    category = category
                                                )
                                            )

                                            if (updated) {
                                                pointsRevision++
                                                editingPoint = null
                                                editSaveFailed = false
                                            } else {
                                                editSaveFailed = true
                                            }

                                        } catch (e: Exception) {
                                            println("Map point update failed: $e")
                                            editSaveFailed = true
                                        }
                                    },

                                    onDismiss = {
                                        editingPoint = null
                                    }
                                )
                            }
                        }

                        if (mapActionFailed) {
                            AlertDialog(
                                onDismissRequest = {
                                    mapActionFailed = false
                                },

                                title = {
                                    Text(
                                        stringResource(
                                            Res.string.map_action_failed
                                        )
                                    )
                                },

                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            mapActionFailed = false
                                        }
                                    ) {
                                        Text(
                                            stringResource(
                                                Res.string.map_ok
                                            )
                                        )
                                    }
                                }
                            )
                        }



                    }


                    "Forecast" -> {
                        SharedForecastScreen(
                            state = forecastState,
                            language = language,

                            onClickDetail = { speciesId ->
                                detailReturnRoute = "Forecast"
                                selectedDetailSpeciesId = speciesId
                                currentRoute = "Detail"
                            },

                            onLocationClick = { _ ->
                                // Подключим переход к точке,
                                // когда перенесём карту.
                            },

                            formatDistance = { distanceKm ->
                                distanceKm.toInt().toString()
                            },

                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (currentRoute) {
                                    "Map" -> stringResource(Res.string.map)
                                    "Settings" -> stringResource(Res.string.settings_screen_title)
                                    "Forecast" -> stringResource(Res.string.forecast_title)
                                    else -> "ForestPoint"
                                }
                            )
                        }
                    }
                }
            }
        }
    }


private fun countDayIos(endMonth: Int): Long {
    val calendar = NSCalendar.currentCalendar
    val today = NSDate()

    return countSeasonDaysLeft(
        endMonth = endMonth,
        currentYear = calendar.component(
            NSCalendarUnitYear,
            fromDate = today
        ).toInt(),
        currentMonth = calendar.component(
            NSCalendarUnitMonth,
            fromDate = today
        ).toInt(),
        currentDay = calendar.component(
            NSCalendarUnitDay,
            fromDate = today
        ).toInt()
    )
}
