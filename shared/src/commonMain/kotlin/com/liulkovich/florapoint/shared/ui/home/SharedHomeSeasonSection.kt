
package com.liulkovich.florapoint.shared.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import com.liulkovich.florapoint.shared.generated.resources.Res
import com.liulkovich.florapoint.shared.generated.resources.days_left
import com.liulkovich.florapoint.shared.generated.resources.no_active_seasons

import com.liulkovich.florapoint.shared.domain.SharedHomeScreenState
import com.liulkovich.florapoint.shared.domain.localizedName
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.liulkovich.florapoint.shared.generated.resources.season_now

@Composable
fun SharedHomeSeasonSection(
    state: SharedHomeScreenState,
    language: String,
    daysLeftCount: (Int) -> Long,
    notificationEnabledIds: Set<Int>,
    onNotificationChange: (Int, Boolean) -> Unit,
    onClickDetail: (Int) -> Unit,
    imageContent: @Composable (String, Modifier) -> Unit
) {
    when {
        state.isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(225.dp)
                    .padding(horizontal = 16.dp)
            ) {
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(4) {
                        SharedHomeSeasonSkeleton()
                    }
                }
            }
        }

        state.species.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.no_active_seasons),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        state.species.size <= 2 -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.species.forEach { speciesItem ->
                    SharedHomeSeasonCard(
                        referenceId = speciesItem.id,
                        imageName = speciesItem.imageName,
                        textName = speciesItem.localizedName(language),
                        daysLeftText = stringResource(
                            Res.string.days_left,
                            daysLeftCount(speciesItem.endMonth)
                        ),
                        notificationEnabled =
                            speciesItem.id in notificationEnabledIds,
                        onNotificationChange = { enabled ->
                            onNotificationChange(speciesItem.id, enabled)
                        },
                        onClickDetail = onClickDetail,
                        imageContent = imageContent
                    )
                }
            }
        }

        else -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(225.dp)
                    .padding(horizontal = 16.dp)
            ) {
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = state.species,
                        key = { it.id }
                    ) { speciesItem ->
                        SharedHomeSeasonCard(
                            referenceId = speciesItem.id,
                            imageName = speciesItem.imageName,
                            textName = speciesItem.localizedName(language),
                            daysLeftText = stringResource(
                                Res.string.days_left,
                                daysLeftCount(speciesItem.endMonth)
                            ),
                            notificationEnabled =
                                speciesItem.id in notificationEnabledIds,
                            onNotificationChange = { enabled ->
                                onNotificationChange(speciesItem.id, enabled)
                            },
                            onClickDetail = onClickDetail,
                            imageContent = imageContent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedHomeSeasonSkeleton() {
    Card(
        modifier = Modifier
            .width(165.dp)
            .height(150.dp),
        shape = RoundedCornerShape(15.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant
                    )
            )

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .width(110.dp)
                    .height(16.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant
                    )
            )

            Spacer(Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .padding(start = 8.dp, bottom = 8.dp)
                    .width(80.dp)
                    .height(14.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}

@Composable
fun SharedHomeSeasonTitle() {
    Text(
        text = stringResource(Res.string.season_now),
        style = MaterialTheme.typography.titleSmall,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(
            start = 16.dp,
            top = 8.dp,
            bottom = 4.dp
        )
    )
}
