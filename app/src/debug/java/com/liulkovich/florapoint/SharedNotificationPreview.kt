
package com.liulkovich.florapoint

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import coil.compose.AsyncImage
import coil.request.ImageRequest

import com.liulkovich.florapoint.domain.localizedName
import com.liulkovich.florapoint.presentation.screens.notifications.NotificationViewModel
import com.liulkovich.florapoint.shared.domain.SharedNotificationSettingsState
import com.liulkovich.florapoint.shared.domain.SharedNotificationSpecies
import com.liulkovich.florapoint.shared.ui.notifications.SharedNotificationScreen

@Composable
fun SharedNotificationPreview(
    modifier: Modifier = Modifier,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val androidState by viewModel.state.collectAsStateWithLifecycle()

    val sharedState = SharedNotificationSettingsState(
        notifyStart = androidState.notifyStart,
        notifyPeak = androidState.notifyPeak,
        notifyEnd = androidState.notifyEnd,
        enabledSpecies = androidState.enabledSpecies.map { species ->
            SharedNotificationSpecies(
                id = species.id,
                displayName = species.localizedName(),
                imageName = species.imageName,
                startMonth = species.startMonth,
                endMonth = species.endMonth
            )
        }
    )

    SharedNotificationScreen(
        modifier = modifier,
        state = sharedState,

        onNotifyStartChange = viewModel::setNotifyStart,

        onNotifyPeakChange = viewModel::setNotifyPeak,

        onNotifyEndChange = viewModel::setNotifyEnd,

        onSpeciesNotificationChange = { speciesId, enabled ->
            viewModel.toggleNotification(
                id = speciesId,
                enabled = if (enabled) 1 else 0
            )
        },

        speciesImageContent = { imageName, imageModifier ->
            val firstImageName = imageName
                .split(",")
                .firstOrNull()
                ?.trim()
                .orEmpty()

            val drawableId = remember(
                context,
                firstImageName
            ) {
                context.resources.getIdentifier(
                    firstImageName,
                    "drawable",
                    context.packageName
                ).takeIf { it != 0 }
                    ?: R.drawable.ic_launcher_background
            }

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(drawableId)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = imageModifier
            )
        }
    )
}
