
package com.liulkovich.florapoint.shared.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.liulkovich.florapoint.shared.domain.SharedHomeScreenState

@Composable
fun SharedHomeScreen(
    state: SharedHomeScreenState,
    language: String,
    notificationEnabledIds: Set<Int>,
    daysLeftCount: (Int) -> Long,
    onClickMap: () -> Unit,
    onClickCategory: (String) -> Unit,
    onSeasonCategorySelected: (String?) -> Unit,
    onNotificationChange: (Int, Boolean) -> Unit,
    onClickDetail: (Int) -> Unit,
    mapContent: @Composable (Modifier) -> Unit,
    categoryImageContent: @Composable (String, Modifier) -> Unit,
    seasonImageContent: @Composable (String, Modifier) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Мини-карта
        item {
            Box(
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp
                )
            ) {
                SharedHomeMapPanel(
                    onClickMap = onClickMap,
                    mapContent = mapContent
                )
            }
        }

        // Заголовок справочника
        item {
            SharedHomeReferenceTitle()
        }

        // Категории
        item {
            SharedHomeCategories(
                onClickCategory = onClickCategory,
                imageContent = categoryImageContent
            )
        }

        // Заголовок сезона и фильтр
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SharedHomeSeasonTitle()

                SharedHomeSeasonCategoryDropdown(
                    selectedCategory = state.selectedSeasonCategory,
                    onCategorySelected = onSeasonCategorySelected
                )
            }
        }

        // Сезонные виды
        item {
            SharedHomeSeasonSection(
                state = state,
                language = language,
                daysLeftCount = daysLeftCount,
                notificationEnabledIds = notificationEnabledIds,
                onNotificationChange = onNotificationChange,
                onClickDetail = onClickDetail,
                imageContent = seasonImageContent
            )
        }

        // Совет дня
        item {
            SharedHomeTipSection(
                tipText = state.tipText
            )
        }

        item {
            Spacer(Modifier.height(10.dp))
        }
    }
}
