
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
import org.jetbrains.compose.resources.stringResource
import com.liulkovich.florapoint.shared.generated.resources.Res
import com.liulkovich.florapoint.shared.generated.resources.forest_all
import com.liulkovich.florapoint.shared.generated.resources.forest_coniferous
import com.liulkovich.florapoint.shared.generated.resources.forest_deciduous
import com.liulkovich.florapoint.shared.generated.resources.forest_mixed

@Composable
fun SharedForestFilter(
    selectedForest: ForestType?,
    onForestSelected: (ForestType?) -> Unit,
    language: String = "ru",
    modifier: Modifier = Modifier
) {
    val options = listOf(
        null to stringResource(Res.string.forest_all),

        ForestType.CONIFEROUS to
                stringResource(Res.string.forest_coniferous),

        ForestType.DECIDUOUS to
                stringResource(Res.string.forest_deciduous),

        ForestType.MIXED to
                stringResource(Res.string.forest_mixed)
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
