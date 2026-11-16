package com.liulkovich.florapoint.shared.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.liulkovich.florapoint.shared.domain.SharedDetailScreenState
import com.liulkovich.florapoint.shared.domain.isDangerousMushroom
import com.liulkovich.florapoint.shared.domain.localizedName
import org.jetbrains.compose.resources.stringResource
import com.liulkovich.florapoint.shared.generated.resources.Res
import com.liulkovich.florapoint.shared.generated.resources.dangerous_mushroom
import com.liulkovich.florapoint.shared.generated.resources.turn_on_notifications
import com.liulkovich.florapoint.shared.generated.resources.turn_off_notifications

@Composable
fun SharedDetailScreen(
    state: SharedDetailScreenState,
    onBack: () -> Unit,
    onNotificationToggle: (Boolean) -> Unit,
    onSpeciesClick: (Int) -> Unit,
    imageContent: @Composable (String, Modifier) -> Unit,
    language: String = "ru"
) {
    val species = state.species
    val listState = rememberLazyListState()

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            SharedDetailTopBar(
                title = species?.localizedName(language) ?: "",
                isNotificationEnabled = state.isNotificationEnabled,
                onBack = onBack,
                onNotificationToggle = onNotificationToggle
            )
        }
    ) { innerPadding ->

        if (species != null) {
            LazyColumn(
                state = listState,
                contentPadding = innerPadding
            ) {
                item(key = "hero") {
                    SharedDetailHeroImage(
                        imageName = species.imageName,
                        name = species.localizedName(language),
                        imageContent = imageContent
                    )
                }

                if (species.isDangerousMushroom()) {
                    item(key = "danger_warning") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = 8.dp
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(20.dp)
                                )

                                Text(
                                    text = stringResource(Res.string.dangerous_mushroom),
                                    color = Color(0xFFFF9800),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item(key = "info") {
                    SharedDetailInfoSection(
                        reference = species,
                        isHighlighted = state.isNotificationEnabled,
                        resolvedLookAlikes = state.resolvedLookAlikes,
                        onSpeciesClick = onSpeciesClick,
                        language = language
                    )
                }

                item(key = "notifications") {
                    Button(
                        onClick = {
                            onNotificationToggle(
                                !state.isNotificationEnabled
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor =
                                if (state.isNotificationEnabled) {
                                    Color(0xFF66BB6A)
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                },
                            contentColor =
                                if (state.isNotificationEnabled) {
                                    Color.White
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                }
                        )
                    ) {
                        Text(
                            text = if (state.isNotificationEnabled) {
                                stringResource(Res.string.turn_off_notifications)
                            } else {
                                stringResource(Res.string.turn_on_notifications)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
