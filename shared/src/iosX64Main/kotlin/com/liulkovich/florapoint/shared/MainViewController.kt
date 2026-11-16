
package com.liulkovich.florapoint.shared

import com.liulkovich.florapoint.shared.ui.components.SharedSpeciesImage
import com.liulkovich.florapoint.shared.domain.ReferenceModel
import platform.Foundation.NSCalendar
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

        var selectedSeasonCategory by remember {
            mutableStateOf<String?>(null)
        }

        val tipText = remember {
            loadRandomFloraTip(language = "ru")
        }
        ForestPointSharedTheme(
            darkTheme = true
        ) {


        Scaffold(
                bottomBar = {
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
            ) { innerPadding ->

                when (currentRoute) {

                    "Home" -> {
                        SharedHomeScreen(
                            state = SharedHomeScreenState(

                                species = remember(selectedSeasonCategory) {
                                    val currentMonth = NSCalendar.currentCalendar.component(
                                        NSCalendarUnitMonth,
                                        fromDate = NSDate()
                                    ).toInt()

                                    loadFloraSpecies()
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

                            notificationEnabledIds = emptySet(),

                            daysLeftCount = { endMonth ->
                                countDayIos(endMonth)
                            },

                            onClickMap = {
                                currentRoute = "Map"
                            },

                            onClickCategory = {
                                currentRoute = "Guide"
                            },

                            onSeasonCategorySelected = { category ->
                                selectedSeasonCategory = category
                            },

                            onNotificationChange = { _, _ -> },

                            onClickDetail = {},

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
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            ForestPointSharedScreen()
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
