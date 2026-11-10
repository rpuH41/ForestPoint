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
                onNotificationToggle = onNotificationToggle,
                backDescription = if (language == "en") "Back" else "Назад",
                notificationDescription = if (language == "en") {
                    "Notifications"
                } else {
                    "Уведомления"
                }
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
                                    text = if (language == "en") {
                                        "Dangerous mushroom"
                                    } else {
                                        "Опасный гриб"
                                    },
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
                                if (language == "en") {
                                    "Turn off notifications"
                                } else {
                                    "Выключить уведомления"
                                }
                            } else {
                                if (language == "en") {
                                    "Turn on notifications"
                                } else {
                                    "Включить уведомления"
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
