
package com.liulkovich.florapoint.shared.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import org.jetbrains.compose.resources.stringResource
import com.liulkovich.florapoint.shared.generated.resources.*

@Composable
fun SharedMapNavigationControls(
    onMyLocation: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false
) {
    val buttonSize = if (isLandscape) 40.dp else 56.dp
    val iconSize = if (isLandscape) 18.dp else 24.dp
    val spacing = if (isLandscape) 4.dp else 8.dp

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        FloatingActionButton(
            onClick = onMyLocation,
            modifier = Modifier.size(buttonSize),
            shape = RoundedCornerShape(14.dp),
            containerColor = MaterialTheme.colorScheme
                .secondaryContainer.copy(alpha = 0.75f),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp
            )
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = stringResource(
                    Res.string.map_center_on_me
                ),
                modifier = Modifier.size(iconSize)
            )
        }

        FloatingActionButton(
            onClick = onZoomIn,
            modifier = Modifier.size(buttonSize),
            shape = RoundedCornerShape(14.dp),
            containerColor = MaterialTheme.colorScheme
                .secondaryContainer.copy(alpha = 0.75f),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(
                    Res.string.map_zoom_in
                ),
                modifier = Modifier.size(iconSize)
            )
        }

        FloatingActionButton(
            onClick = onZoomOut,
            modifier = Modifier.size(buttonSize),
            shape = RoundedCornerShape(14.dp),
            containerColor = MaterialTheme.colorScheme
                .secondaryContainer.copy(alpha = 0.75f),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp
            )
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = stringResource(
                    Res.string.map_zoom_out
                ),
                modifier = Modifier.size(iconSize)
            )
        }
    }
}