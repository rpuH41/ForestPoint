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
import com.liulkovich.florapoint.shared.domain.seasonText

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
            title = if (language == "en") "Where it grows" else "Где растёт",
            text = reference.localizedHabitat(language),
            isHighlighted = isHighlighted
        )

        SharedDetailInfoBlock(
            icon = Icons.Default.Info,
            title = if (language == "en") "Description" else "Описание",
            text = reference.localizedDescription(language),
            isHighlighted = isHighlighted
        )

        SharedLookAlikesBlock(
            resolvedLookAlikes = resolvedLookAlikes,
            isHighlighted = isHighlighted,
            onSpeciesClick = onSpeciesClick,
            language = language
        )

        if (!reference.differencesRu.isNullOrBlank()) {
            SharedDetailInfoBlock(
                icon = Icons.Default.Info,
                title = if (language == "en") "Differences" else "Чем отличается",
                text = reference.localizedDifferences(language),
                isHighlighted = isHighlighted
            )
        }

        SharedDetailInfoBlock(
            icon = Icons.Default.DateRange,
            title = if (language == "en") "Harvest period" else "Период сбора",
            text = reference.seasonText(language),
            isHighlighted = isHighlighted
        )
    }
}
