
package com.liulkovich.florapoint.shared.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liulkovich.florapoint.shared.ui.theme.ForestDarkGreen

data class ForestPointNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun ForestPointBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onForecastClick: () -> Unit,
    compact: Boolean = false
) {
    val items = listOf(
        ForestPointNavItem("Главная", Icons.Default.Home, "Home"),
        ForestPointNavItem("Карта", Icons.Default.LocationOn, "Map"),
        ForestPointNavItem("Справочник", Icons.AutoMirrored.Filled.List, "Guide"),
        ForestPointNavItem("Настройки", Icons.Default.Settings, "Settings")
    )

    val barHeight = if (compact) 64.dp else 100.dp
    val fabSize = if (compact) 44.dp else 56.dp
    val fabOffset = if (compact) 4.dp else 10.dp
    val fabEmojiSize = if (compact) 18.sp else 24.sp
    val centerGapWidth = if (compact) 48.dp else 64.dp

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 24.dp
            ),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.take(2).forEach { item ->
                    ForestPointNavigationItem(
                        item = item,
                        selected = currentRoute.startsWith(item.route),
                        onClick = { onNavigate(item.route) },
                        modifier = Modifier.weight(1f),
                        compact = compact
                    )
                }

                Box(
                    modifier = Modifier.width(centerGapWidth)
                )

                items.drop(2).forEach { item ->
                    ForestPointNavigationItem(
                        item = item,
                        selected = currentRoute.startsWith(item.route),
                        onClick = { onNavigate(item.route) },
                        modifier = Modifier.weight(1f),
                        compact = compact
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onForecastClick,
            shape = CircleShape,
            containerColor = ForestDarkGreen,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = fabOffset)
                .size(fabSize)
        ) {
            Text(
                text = "🍄",
                fontSize = fabEmojiSize
            )
        }
    }
}

@Composable
private fun ForestPointNavigationItem(
    item: ForestPointNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val density = LocalDensity.current

    val color = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val iconSize = if (compact) 18.dp else 24.dp
    val verticalPadding = if (compact) 4.dp else 8.dp

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = verticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = color,
            modifier = Modifier.size(iconSize)
        )

        if (!compact) {
            Text(
                text = item.label,
                color = color,
                fontSize = with(density) {
                    (12.sp.value / fontScale).sp
                },
                maxLines = 1
            )
        }
    }
}
