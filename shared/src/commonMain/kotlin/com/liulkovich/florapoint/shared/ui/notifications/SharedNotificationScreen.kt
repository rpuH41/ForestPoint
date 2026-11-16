
package com.liulkovich.florapoint.shared.ui.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.liulkovich.florapoint.shared.domain.SharedNotificationSettingsState
import com.liulkovich.florapoint.shared.domain.SharedNotificationSpecies
import com.liulkovich.florapoint.shared.generated.resources.Res
import com.liulkovich.florapoint.shared.generated.resources.*

import org.jetbrains.compose.resources.stringResource

private val NotificationGreen = Color(0xFF4CAF50)

@Composable
fun SharedNotificationScreen(
    state: SharedNotificationSettingsState,
    onNotifyStartChange: (Boolean) -> Unit,
    onNotifyPeakChange: (Boolean) -> Unit,
    onNotifyEndChange: (Boolean) -> Unit,
    onSpeciesNotificationChange: (Int, Boolean) -> Unit,
    speciesImageContent: @Composable (String, Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    val allDisabled =
        !state.notifyStart &&
                !state.notifyPeak &&
                !state.notifyEnd

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = stringResource(
                    Res.string.notification_seasonal_title
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = stringResource(
                    Res.string.notification_seasonal_description
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item {
            HorizontalDivider()
        }

        item {
            SharedNotificationSwitch(
                title = stringResource(
                    Res.string.notification_start
                ),
                subtitle = stringResource(
                    Res.string.notification_start_description
                ),
                checked = state.notifyStart,
                onCheckedChange = onNotifyStartChange
            )
            HorizontalDivider()
        }

        item {
            SharedNotificationSwitch(
                title = stringResource(
                    Res.string.notification_peak
                ),
                subtitle = stringResource(
                    Res.string.notification_peak_description
                ),
                checked = state.notifyPeak,
                onCheckedChange = onNotifyPeakChange
            )
            HorizontalDivider()
        }

        item {
            SharedNotificationSwitch(
                title = stringResource(
                    Res.string.notification_end
                ),
                subtitle = stringResource(
                    Res.string.notification_end_description
                ),
                checked = state.notifyEnd,
                onCheckedChange = onNotifyEndChange
            )
            HorizontalDivider()
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (allDisabled) {
                        Icons.Default.NotificationsOff
                    } else {
                        Icons.Default.NotificationsActive
                    },
                    contentDescription = null,
                    tint = if (allDisabled) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        NotificationGreen
                    }
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = if (allDisabled) {
                        stringResource(
                            Res.string.notification_disabled
                        )
                    } else {
                        stringResource(
                            Res.string.notification_enabled_hint
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()
        }

        item {
            Text(
                text = if (state.enabledSpecies.isEmpty()) {
                    stringResource(
                        Res.string.notification_no_species
                    )
                } else {
                    stringResource(
                        Res.string.notification_species_count,
                        state.enabledSpecies.size
                    )
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (state.enabledSpecies.isEmpty()) {
            item {
                Text(
                    text = stringResource(
                        Res.string.notification_enable_in_guide
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(
                items = state.enabledSpecies,
                key = { it.id }
            ) { species ->
                SharedNotificationSpeciesItem(
                    species = species,
                    imageContent = speciesImageContent,
                    onDisable = {
                        onSpeciesNotificationChange(
                            species.id,
                            false
                        )
                    }
                )
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SharedNotificationSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = NotificationGreen
            )
        )
    }
}

@Composable
private fun SharedNotificationSpeciesItem(
    species: SharedNotificationSpecies,
    imageContent: @Composable (String, Modifier) -> Unit,
    onDisable: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            imageContent(
                species.imageName,
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = species.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = stringResource(
                        Res.string.notification_species_season,
                        species.startMonth,
                        species.endMonth
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDisable) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = stringResource(
                        Res.string.notification_disable_species
                    ),
                    tint = NotificationGreen
                )
            }
        }
    }
}
