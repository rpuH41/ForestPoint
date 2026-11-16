
package com.liulkovich.florapoint.shared

import com.liulkovich.florapoint.shared.domain.SharedGuideCommand
import com.liulkovich.florapoint.shared.domain.SharedGuideScreenState
import com.liulkovich.florapoint.shared.ui.guide.SharedGuideScreen
import com.liulkovich.florapoint.shared.ui.components.SharedSpeciesImage
import com.liulkovich.florapoint.shared.domain.ReferenceModel
import platform.Foundation.NSCalendar
import com.liulkovich.florapoint.shared.domain.SharedDetailScreenState
import com.liulkovich.florapoint.shared.ui.detail.SharedDetailScreen
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSDate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.liulkovich.florapoint.shared.domain.SharedFloraCategory
import com.liulkovich.florapoint.shared.domain.SharedHomeScreenState
import com.liulkovich.florapoint.shared.ui.ForestPointSharedScreen
import com.liulkovich.florapoint.shared.ui.components.ForestPointBottomBar
import com.liulkovich.florapoint.shared.ui.home.SharedHomeScreen
import com.liulkovich.florapoint.shared.ui.theme.ForestPointSharedTheme
import platform.UIKit.UIViewController
import com.liulkovich.florapoint.shared.domain.countSeasonDaysLeft
import platform.Foundation.NSCalendarUnitDay
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import androidx.compose.runtime.remember
import com.liulkovich.florapoint.shared.generated.resources.Res
import com.liulkovich.florapoint.shared.generated.resources.mushroom
import com.liulkovich.florapoint.shared.generated.resources.berry
import com.liulkovich.florapoint.shared.generated.resources.plant
import com.liulkovich.florapoint.shared.generated.resources.nut
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDateComponents
import platform.Foundation.timeIntervalSince1970

fun MainViewController(): UIViewController =
    ComposeUIViewController {

        var currentRoute by remember {
            mutableStateOf("Home")
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
