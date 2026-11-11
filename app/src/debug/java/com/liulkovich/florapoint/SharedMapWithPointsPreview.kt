
package com.liulkovich.florapoint

import android.content.res.Configuration
import android.widget.Toast

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import com.liulkovich.florapoint.domain.LocationUtils
import com.liulkovich.florapoint.presentation.components.AddPointSheetContent
import com.liulkovich.florapoint.presentation.components.EditPointSheetContent
import com.liulkovich.florapoint.presentation.screens.map.BottomSheetMode
import com.liulkovich.florapoint.presentation.screens.map.MapViewModel
import com.liulkovich.florapoint.shared.ui.map.SharedMapPointsList

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedMapWithPointsPreview(
    modifier: Modifier = Modifier
) {
    val viewModel: MapViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val isLandscape =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val locale = configuration.locales[0] ?: Locale.getDefault()
    val language = locale.language

    val sharedState = state.toSharedMapPointsState(
        language = language,
        customSpeciesName = stringResource(R.string.custom_species),
        unknownSpeciesName = stringResource(R.string.unknown_species),
        isPublicForeignPoint = viewModel::isPublicForeignPoint
    )

    val originalPointsByKey = state.sortedPointsWithDistance
        .associate { (point, _) ->
            val key = if (point.id != 0) {
                "local_${point.id}"
            } else {
                "public_${point.cloudId ?: point.hashCode()}"
            }

            key to point
        }

    // Одна и та же карта для обеих ориентаций.
    val mapPane: @Composable (Modifier) -> Unit = { paneModifier ->
        SharedMapPreview(
            modifier = paneModifier
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp)),
            mapViewModel = viewModel
        )
    }

    // Один и тот же список для обеих ориентаций.
    val pointsPane: @Composable (Modifier) -> Unit = { paneModifier ->
        SharedMapPointsList(
            state = sharedState,
            modifier = paneModifier,

            onSearchQueryChange = viewModel::onSearchQueryChanged,

            onPointClick = { sharedPoint ->
                originalPointsByKey[sharedPoint.key]?.let { point ->
                    // Центрирование карты для любой точки.
                    viewModel.onPointClicked(point)

                    // Для чужой публичной точки также открываем сведения.
                    if (viewModel.isPublicForeignPoint(point)) {
                        viewModel.onPublicPointClicked(point)
                    }
                }
            },

            onPointLongClick = { sharedPoint ->
                originalPointsByKey[sharedPoint.key]?.let { point ->
                    if (!viewModel.isPublicForeignPoint(point)) {
                        viewModel.onPointLongClicked(point)
                    }
                }
            },

            onEdit = { sharedPoint ->
                originalPointsByKey[sharedPoint.key]?.let { point ->
                    if (!viewModel.isPublicForeignPoint(point)) {
                        viewModel.onPointLongClicked(point)
                    }
                }
            },

            onDelete = { sharedPoint ->
                originalPointsByKey[sharedPoint.key]?.let { point ->
                    if (!viewModel.isPublicForeignPoint(point)) {
                        viewModel.deletePoint(point.id)
                    }
                }
            },

            onShare = { sharedPoint ->
                originalPointsByKey[sharedPoint.key]?.let { point ->
                    viewModel.sharePoint(
                        context,
                        point,
                        sharedPoint.displayName
                    )
                }
            },

            formatDate = { timestampSeconds ->
                SimpleDateFormat(
                    "dd MMM yyyy",
                    locale
                ).format(Date(timestampSeconds * 1000L))
            },

            formatDistance = { distance ->
                LocationUtils.formatDistance(distance)
            }
        )
    }


    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val mapWidth = if (isLandscape) {
            maxWidth * (2f / 3f)
        } else {
            maxWidth
        }

        val mapHeight = if (isLandscape) {
            maxHeight
        } else {
            maxHeight * (2f / 3f)
        }

        val listWidth = if (isLandscape) {
            maxWidth - mapWidth
        } else {
            maxWidth
        }

        val listHeight = if (isLandscape) {
            maxHeight
        } else {
            maxHeight - mapHeight
        }

        // Карта всегда находится в одном месте дерева Compose.
        // При повороте меняются только размеры.
        mapPane(
            Modifier
                .align(Alignment.TopStart)
                .size(mapWidth, mapHeight)
        )

        if (isLandscape) {
            VerticalDivider(
                modifier = Modifier
                    .offset(x = mapWidth)
                    .height(maxHeight)
            )
        } else {
            HorizontalDivider(
                modifier = Modifier
                    .offset(y = mapHeight)
                    .width(maxWidth)
            )
        }

        // Список также не пересоздаётся при повороте.
        pointsPane(
            Modifier
                .offset(
                    x = if (isLandscape) mapWidth else 0.dp,
                    y = if (isLandscape) 0.dp else mapHeight
                )
                .size(listWidth, listHeight)
        )
    }

    when (val mode = state.bottomSheetMode) {
        is BottomSheetMode.Add -> {
            ModalBottomSheet(
                onDismissRequest = viewModel::dismissBottomSheet
            ) {
                AddPointSheetContent(
                    species = state.species,
                    initialName = state.deepLinkName,
                    initialCategory = state.deepLinkCategory,

                    onSave = {
                            speciesId,
                            userName,
                            description,
                            category,
                            isPublic ->

                        viewModel.addNewPoint(
                            mode.latitude,
                            mode.longitude,
                            speciesId,
                            userName,
                            description,
                            category,
                            isPublic
                        )

                        viewModel.dismissBottomSheet()
                    },

                    onDismiss = viewModel::dismissBottomSheet,
                    isAuthorized = viewModel.isAuthorized(),

                    onOpenSettings = {
                        Toast.makeText(
                            context,
                            if (language == "ru") {
                                "Откройте настройки в основном приложении"
                            } else {
                                "Open settings in the main app"
                            },
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }

        is BottomSheetMode.Edit -> {
            val point = state.userPoints.find {
                it.id == mode.pointId
            }

            if (point != null) {
                ModalBottomSheet(
                    onDismissRequest = viewModel::dismissBottomSheet
                ) {
                    EditPointSheetContent(
                        point = point,
                        species = state.species,

                        onSave = {
                                speciesId,
                                userName,
                                description,
                                category,
                                isPublic ->

                            viewModel.updateUserPoint(
                                mode.pointId,
                                speciesId,
                                userName,
                                description,
                                category,
                                isPublic
                            )

                            viewModel.dismissBottomSheet()
                        },

                        onDelete = {
                            viewModel.deletePoint(mode.pointId)
                            viewModel.dismissBottomSheet()
                        },

                        onDismiss = viewModel::dismissBottomSheet,
                        isAuthorized = viewModel.isAuthorized(),

                        onOpenSettings = {
                            Toast.makeText(
                                context,
                                if (language == "ru") {
                                    "Откройте настройки в основном приложении"
                                } else {
                                    "Open settings in the main app"
                                },
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }
            }
        }

        is BottomSheetMode.ViewPublic -> {
            ModalBottomSheet(
                onDismissRequest = viewModel::dismissBottomSheet
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = mode.point.userName,
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (mode.point.description.isNotBlank()) {
                        Text(
                            text = mode.point.description,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        else -> Unit
    }
}
