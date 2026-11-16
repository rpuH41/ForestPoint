
package com.liulkovich.florapoint

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.liulkovich.florapoint.presentation.screens.settings.SettingsViewModel
import com.liulkovich.florapoint.shared.domain.SharedOfflineRegion
import com.liulkovich.florapoint.shared.domain.SharedOfflineRegionsState
import com.liulkovich.florapoint.shared.ui.settings.SharedOfflineRegionsScreen

import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun SharedOfflineRegionsPreview(
    viewModel: SettingsViewModel,
    onNavigateToDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val regions by viewModel.offlineRegions
        .collectAsStateWithLifecycle()

    val locale = LocalConfiguration.current.locales[0]

    val sharedState = SharedOfflineRegionsState(
        regions = regions.map { region ->
            val sizeMb =
                region.sizeBytes / (1024.0 * 1024.0)

            val sizeLabel = String.format(
                locale,
                "%.1f MB",
                sizeMb
            )

            val createdAtLabel = SimpleDateFormat(
                "dd.MM.yyyy",
                locale
            ).format(Date(region.createdAt))

            SharedOfflineRegion(
                id = region.id,
                name = region.name,
                tilesCount = region.tilesCount,
                sizeLabel = sizeLabel,
                createdAtLabel = createdAtLabel
            )
        }
    )

    SharedOfflineRegionsScreen(
        modifier = modifier,
        state = sharedState,

        onNavigateToDownload = onNavigateToDownload,

        onDeleteRegion = { regionId ->
            viewModel.deleteRegion(regionId)
        }
    )
}
