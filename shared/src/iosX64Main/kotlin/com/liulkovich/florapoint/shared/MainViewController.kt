
package com.liulkovich.florapoint.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
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

fun MainViewController(
    socialGateway: SharedSocialForecastGateway
): UIViewController =
    ComposeUIViewController {

        var currentRoute by remember {
            mutableStateOf("Home")
        }

        var weather by remember {
            mutableStateOf<SharedWeatherData?>(null)
        }

        var forecastState by remember {
            mutableStateOf(SharedForecastScreenState())
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

        val tipText = remember {
            loadRandomFloraTip(language = "ru")
        }

        LaunchedEffect(currentRoute) {

            if (currentRoute != "Forecast") {
                return@LaunchedEffect
            }

            forecastState = SharedForecastScreenState(
                isLoading = true
            )

            try {
                // Временно: тестовые координаты Москвы.
                // Позже заменим на CLLocationManager.

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

                val language = "ru"

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

                            language = "ru",


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

                                language = "ru",

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

                                language = "ru",


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



                    "Forecast" -> {
                        SharedForecastScreen(
                            state = forecastState,
                            language = "ru",

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
                                    "Map" -> "🗺️ Карта ForestPoint"
                                    "Settings" -> "⚙️ Настройки"
                                    "Forecast" -> "🍄 Прогноз"
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
