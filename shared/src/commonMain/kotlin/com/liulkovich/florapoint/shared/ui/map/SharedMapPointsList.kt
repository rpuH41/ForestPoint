
package com.liulkovich.florapoint.shared.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import org.jetbrains.compose.resources.stringResource

import com.liulkovich.florapoint.shared.domain.SharedMapPoint
import com.liulkovich.florapoint.shared.domain.SharedMapPointsState
import com.liulkovich.florapoint.shared.generated.resources.*

@Composable
fun SharedMapPointsList(
    state: SharedMapPointsState,
    onSearchQueryChange: (String) -> Unit,
    onPointClick: (SharedMapPoint) -> Unit,
    onPointLongClick: (SharedMapPoint) -> Unit,
    onEdit: (SharedMapPoint) -> Unit,
    onDelete: (SharedMapPoint) -> Unit,
    onShare: (SharedMapPoint) -> Unit,
    formatDate: (Long) -> String,
    formatDistance: (Double) -> String,
    modifier: Modifier = Modifier
) {
    val displayPoints = remember(
        state.points,
        state.searchQuery
    ) {
        val query = state.searchQuery.trim().lowercase()

        if (query.isEmpty()) {
            state.points
        } else {
            state.points.filter { point ->
                point.displayName.lowercase().contains(query) ||
                        point.userName.lowercase().contains(query)
            }
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(
        state.selectedPointKey,
        displayPoints
    ) {
        val selectedKey =
            state.selectedPointKey ?: return@LaunchedEffect

        val selectedIndex = displayPoints.indexOfFirst { point ->
            point.key == selectedKey
        }

        if (selectedIndex >= 0) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Column(
        modifier = modifier
    ) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 6.dp
                ),
            placeholder = {
                Text(
                    text = stringResource(
                        Res.string.search_places
                    ),
                    fontSize = 13.sp
                )
            },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onSearchQueryChange("")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(
                                Res.string.clear
                            ),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 13.sp
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = 12.dp,
                vertical = 4.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            state = listState
        ) {
            items(
                items = displayPoints,
                key = { point ->
                    point.key
                }
            ) { point ->

                SharedMapPointCard(
                    point = point,

                    formatDate = formatDate,
                    formatDistance = formatDistance,

                    onClick = {
                        onPointClick(point)
                    },

                    onLongClick = {
                        onPointLongClick(point)
                    },

                    onEdit = {
                        onEdit(point)
                    },

                    onDelete = {
                        onDelete(point)
                    },

                    onShare = {
                        onShare(point)
                    }
                )
            }
        }
    }
}
