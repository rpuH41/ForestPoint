package com.liulkovich.florapoint.shared.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.liulkovich.florapoint.shared.domain.ReferenceModel
import com.liulkovich.florapoint.shared.domain.localizedDescription
import com.liulkovich.florapoint.shared.domain.localizedDifferences
import com.liulkovich.florapoint.shared.domain.localizedHabitat
import org.jetbrains.compose.resources.stringResource

import com.liulkovich.florapoint.shared.generated.resources.Res
import com.liulkovich.florapoint.shared.generated.resources.what_is_the_habitat_of
import com.liulkovich.florapoint.shared.generated.resources.description
import com.liulkovich.florapoint.shared.generated.resources.differences_from_original
import com.liulkovich.florapoint.shared.generated.resources.harvest_period

@Composable
fun SharedDetailInfoSection(
    reference: ReferenceModel,
    isHighlighted: Boolean,
    resolvedLookAlikes: Map<String, Int?> = emptyMap(),
    onSpeciesClick: (Int) -> Unit = {},
    language: String = "ru"
) {
    Column(
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            top = 4.dp,
            bottom = 16.dp
        )
    ) {
        SharedDetailInfoBlock(
            icon = Icons.Default.LocationOn,
            title = stringResource(Res.string.what_is_the_habitat_of),
            text = reference.localizedHabitat(language),
            isHighlighted = isHighlighted
        )

        SharedDetailInfoBlock(
            icon = Icons.Default.Info,
            title = stringResource(Res.string.description),
            text = reference.localizedDescription(language),
            isHighlighted = isHighlighted
        )

        SharedLookAlikesBlock(
            resolvedLookAlikes = resolvedLookAlikes,
            isHighlighted = isHighlighted,
            onSpeciesClick = onSpeciesClick
        )

        if (!reference.differencesRu.isNullOrBlank()) {
            SharedDetailInfoBlock(
                icon = Icons.Default.Info,
                title = stringResource(Res.string.differences_from_original),
                text = reference.localizedDifferences(language),
                isHighlighted = isHighlighted
            )
        }

        SharedDetailInfoBlock(
            icon = Icons.Default.DateRange,
            title = stringResource(Res.string.harvest_period),
            text = sharedDetailSeasonText(
                startMonth = reference.startMonth,
                endMonth = reference.endMonth
            ),
            isHighlighted = isHighlighted
        )
    }
}
