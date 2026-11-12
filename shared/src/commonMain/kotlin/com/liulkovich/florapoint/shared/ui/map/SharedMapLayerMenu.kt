
package com.liulkovich.florapoint.shared.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

import org.jetbrains.compose.resources.stringResource

import com.liulkovich.florapoint.shared.domain.SharedGoogleMapLayer
import com.liulkovich.florapoint.shared.domain.SharedMapProvider
import com.liulkovich.florapoint.shared.domain.SharedMapSettings
import com.liulkovich.florapoint.shared.generated.resources.*

@Composable
fun SharedMapLayerMenu(
    settings: SharedMapSettings,
    onProviderSelected: (SharedMapProvider) -> Unit,
    onGoogleLayerSelected: (SharedGoogleMapLayer) -> Unit,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    val fabSize = if (isLandscape) 40.dp else 56.dp
    val iconSize = if (isLandscape) 18.dp else 24.dp

    Box(modifier = modifier) {
        FloatingActionButton(
            onClick = {
                expanded = true
            },
            modifier = Modifier
                .size(fabSize)
                .graphicsLayer {
                    alpha = 0.75f
                    compositingStrategy =
                        CompositingStrategy.Offscreen
                },
            containerColor =
                MaterialTheme.colorScheme.secondaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp
            )
        ) {
            Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = stringResource(
                    Res.string.map_layers_description
                ),
                modifier = Modifier.size(iconSize)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
            shape = RoundedCornerShape(16.dp),
            containerColor =
                MaterialTheme.colorScheme.secondaryContainer.copy(
                    alpha = 0.85f
                ),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            DropdownMenuItem(
                text = {
                    val title = stringResource(
                        Res.string.map_provider_osm
                    )

                    Text(
                        text = if (
                            settings.provider == SharedMapProvider.OSM
                        ) {
                            stringResource(
                                Res.string.map_layer_selected,
                                title
                            )
                        } else {
                            title
                        }
                    )
                },
                onClick = {
                    onProviderSelected(SharedMapProvider.OSM)
                    expanded = false
                }
            )

            DropdownMenuItem(
                text = {
                    val title = stringResource(
                        Res.string.map_provider_google
                    )

                    Text(
                        text = if (
                            settings.provider == SharedMapProvider.GOOGLE
                        ) {
                            stringResource(
                                Res.string.map_layer_selected,
                                title
                            )
                        } else {
                            title
                        }
                    )
                },
                onClick = {
                    onProviderSelected(SharedMapProvider.GOOGLE)
                    expanded = false
                }
            )

            if (settings.provider == SharedMapProvider.GOOGLE) {
                HorizontalDivider()

                SharedGoogleMapLayer.entries.forEach { layer ->
                    DropdownMenuItem(
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(
                            horizontal = 12.dp
                        ),
                        text = {
                            val title = when (layer) {
                                SharedGoogleMapLayer.NORMAL ->
                                    stringResource(
                                        Res.string.google_map_layer_normal
                                    )

                                SharedGoogleMapLayer.SATELLITE ->
                                    stringResource(
                                        Res.string.google_map_layer_satellite
                                    )

                                SharedGoogleMapLayer.HYBRID ->
                                    stringResource(
                                        Res.string.google_map_layer_hybrid
                                    )

                                SharedGoogleMapLayer.TERRAIN ->
                                    stringResource(
                                        Res.string.google_map_layer_terrain
                                    )
                            }

                            Text(
                                text = if (
                                    settings.googleLayer == layer
                                ) {
                                    stringResource(
                                        Res.string.map_layer_selected,
                                        title
                                    )
                                } else {
                                    title
                                }
                            )
                        },
                        onClick = {
                            onGoogleLayerSelected(layer)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
