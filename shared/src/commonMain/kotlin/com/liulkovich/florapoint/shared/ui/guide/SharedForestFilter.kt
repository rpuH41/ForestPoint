
package com.liulkovich.florapoint.shared.ui.guide

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.liulkovich.florapoint.shared.domain.ForestType

@Composable
fun SharedForestFilter(
    selectedForest: ForestType?,
    onForestSelected: (ForestType?) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        null to "Все",
        ForestType.CONIFEROUS to "🌲 Хвойный",
        ForestType.DECIDUOUS to "🌳 Лиственный",
        ForestType.MIXED to "🌲 Смешанный"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 24.dp,
                vertical = 2.dp
            )
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (forest, label) ->
            FilterChip(
                selected = selectedForest == forest,
                onClick = {
                    onForestSelected(forest)
                },
                label = {
                    Text(label)
                }
            )
        }
    }
}
