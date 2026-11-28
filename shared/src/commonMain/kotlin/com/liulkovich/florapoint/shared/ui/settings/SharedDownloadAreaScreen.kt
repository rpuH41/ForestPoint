
package com.liulkovich.florapoint.shared.ui.settings

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntRect
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import com.liulkovich.florapoint.shared.domain.SharedDownloadAreaEstimate
import com.liulkovich.florapoint.shared.domain.SharedDownloadAreaState
import com.liulkovich.florapoint.shared.generated.resources.*

import org.jetbrains.compose.resources.stringResource

import kotlin.math.roundToInt

private val DownloadGreen = Color(0xFF1B5E20)
private val DownloadLightGreen = Color(0xFF66BB6A)

@Composable
fun SharedDownloadAreaScreen(
    state: SharedDownloadAreaState,
    mapContent: @Composable (Modifier) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onRecenter: () -> Unit,
    onEstimateRequested: () -> SharedDownloadAreaEstimate,
    onDownloadRequested: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSelectionBoundsChanged: (IntRect) -> Unit = {}
) {
    var showNameDialog by remember {
        mutableStateOf(false)
    }

    var regionName by remember {
        mutableStateOf("")
    }

    var estimate by remember {
        mutableStateOf(SharedDownloadAreaEstimate())
    }

    var mapBounds by remember {
        mutableStateOf<Rect?>(null)
    }

    var selectionBounds by remember {
        mutableStateOf<Rect?>(null)
    }

    LaunchedEffect(mapBounds, selectionBounds) {
        val map = mapBounds ?: return@LaunchedEffect
        val selection = selectionBounds ?: return@LaunchedEffect

        onSelectionBoundsChanged(
            IntRect(
                left = (selection.left - map.left).roundToInt(),
                top = (selection.top - map.top).roundToInt(),
                right = (selection.right - map.left).roundToInt(),
                bottom = (selection.bottom - map.top).roundToInt()
            )
        )
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Text(
            text = stringResource(
                Res.string.download_area_title
            ),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp
            )
        )

        Text(
            text = stringResource(
                Res.string.download_area_instruction
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 4.dp
            )
        )

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            // Платформенная карта:
            // Android -> osmdroid
            // iOS -> будущая реализация
            mapContent(
                Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        mapBounds = coordinates.boundsInRoot()
                    }
            )

            // Уменьшаем рамку при небольшом
            // размере экрана и в landscape.
            val selectionSize = minOf(
                260.dp,
                maxWidth * 0.65f,
                maxHeight * 0.75f
            )

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(selectionSize)
                        .border(
                            width = 2.dp,
                            color = DownloadGreen,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .onGloballyPositioned { coordinates ->
                            selectionBounds = coordinates.boundsInRoot()
                        }
                )

                Text(
                    text = stringResource(
                        Res.string.download_area_selection
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = DownloadGreen,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = onRecenter,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationSearching,
                        contentDescription = stringResource(
                            Res.string.download_area_location
                        )
                    )
                }

                FloatingActionButton(
                    onClick = onZoomIn,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(
                            Res.string.download_area_zoom_in
                        )
                    )
                }

                FloatingActionButton(
                    onClick = onZoomOut,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = stringResource(
                            Res.string.download_area_zoom_out
                        )
                    )
                }
            }
        }

        val progress = state.progress

        if (state.isDownloading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (progress != null && progress.total > 0) {
                    val fraction = (
                            progress.current.toFloat() /
                                    progress.total.toFloat()
                            ).coerceIn(0f, 1f)

                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = stringResource(
                            Res.string.download_area_progress,
                            progress.current,
                            progress.total
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        if (progress?.isError == true) {
            Text(
                text = stringResource(
                    Res.string.download_area_error
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(8.dp))
        }

        if (progress?.isFinished == true &&
            !state.isDownloading
        ) {
            Text(
                text = stringResource(
                    Res.string.download_area_success
                ),
                style = MaterialTheme.typography.bodySmall,
                color = DownloadGreen,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = {
                estimate = onEstimateRequested()
                showNameDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            enabled = !state.isDownloading,
            colors = ButtonDefaults.buttonColors(
                containerColor = DownloadGreen
            )
        ) {
            Text(
                text = if (state.isDownloading) {
                    stringResource(
                        Res.string.download_area_loading
                    )
                } else {
                    stringResource(
                        Res.string.download_area_button
                    )
                },
                color = Color.White
            )
        }
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = {
                showNameDialog = false
            },
            title = {
                Text(
                    stringResource(
                        Res.string.download_area_region_name
                    )
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(
                            Res.string.download_area_name_hint
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = regionName,
                        onValueChange = {
                            regionName = it
                        },
                        placeholder = {
                            Text(
                                stringResource(
                                    Res.string.download_area_name_example
                                )
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = stringResource(
                            Res.string.download_area_size,
                            estimate.areaKm2.toOneDecimal()
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = DownloadGreen
                    )

                    Text(
                        text = stringResource(
                            Res.string.download_area_estimate,
                            estimate.approximateSizeMb.toOneDecimal(),
                            estimate.tilesCount
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = regionName.trim()

                        if (name.isNotEmpty()) {
                            showNameDialog = false
                            regionName = ""
                            onDownloadRequested(name)
                        }
                    },
                    enabled = regionName.isNotBlank()
                ) {
                    Text(
                        text = stringResource(
                            Res.string.download_area_download
                        ),
                        color = DownloadLightGreen
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNameDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(
                            Res.string.download_area_cancel
                        ),
                        color = DownloadLightGreen
                    )
                }
            }
        )
    }
}

private fun Double.toOneDecimal(): String {
    return ((this * 10.0).roundToInt() / 10.0).toString()
}
