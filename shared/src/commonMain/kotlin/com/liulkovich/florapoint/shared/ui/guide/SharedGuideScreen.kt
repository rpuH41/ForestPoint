
package com.liulkovich.florapoint.shared.ui.guide

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.liulkovich.florapoint.shared.domain.SharedGuideCommand
import com.liulkovich.florapoint.shared.domain.SharedGuideScreenState
import com.liulkovich.florapoint.shared.domain.localizedName
import com.liulkovich.florapoint.shared.domain.isDangerousMushroom
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.liulkovich.florapoint.shared.domain.ForestType
import com.liulkovich.florapoint.shared.domain.matchesForest

@Composable
fun SharedGuideScreen(
    state: SharedGuideScreenState,
    onCommand: (SharedGuideCommand) -> Unit,
    onClickDetail: (Int) -> Unit,
    imageContent: @Composable (String, Modifier) -> Unit,
    language: String = "ru"
) {
    var selectedForestName by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val selectedForest = selectedForestName?.let { name ->
        ForestType.entries.find { it.name == name }
    }

    val filteredSpecies = state.species.filter {
        it.matchesForest(selectedForest)
    }
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Spacer(Modifier.height(24.dp))

        GuideSearchBar(
            query = state.query,
            onQueryChange = {
                onCommand(SharedGuideCommand.InputSearchQuery(it))
            },
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(12.dp))

        GuideCategoryFilter(
            selectedCategories = state.selectedCategories,
            onCategoryChange = { category ->
                selectedForestName = null

                onCommand(
                    SharedGuideCommand.CheckCategory(category)
                )
            }
        )

        if (state.selectedCategories == setOf("mushroom")) {
            SharedForestFilter(
                selectedForest = selectedForest,
                onForestSelected = { forest ->
                    selectedForestName = forest?.name
                },
                language = language
            )
        }

        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            LazyColumn {

                if (
                    state.selectedCategories.isEmpty() ||
                    "mushroom" in state.selectedCategories
                ) {
                    item(key = "safety_notice") {
                        SharedGuideSafetyNotice(
                            modifier = Modifier.padding(
                                horizontal = 24.dp,
                                vertical = 10.dp
                            )
                        )
                    }
                }

                items(
                    items = filteredSpecies,
                    key = { it.id }
                ){ reference ->

                    SharedGuideCard(
                        reference = reference,
                        language = language,
                        seasonText = sharedGuideSeasonText(
                            startMonth = reference.startMonth,
                            endMonth = reference.endMonth
                        ),
                        dangerous = reference.isDangerousMushroom(),
                        onNotificationChange = { enabled ->
                            onCommand(
                                SharedGuideCommand.ToggleNotification(
                                    id = reference.id,
                                    enabled = if (enabled) 1 else 0
                                )
                            )
                        },

                        onClick = {
                            onClickDetail(reference.id)
                        },
                        imageContent = imageContent,
                        modifier = Modifier.padding(
                            horizontal = 24.dp,
                            vertical = 8.dp
                        )
                    )
                }

            }
        }
    }
}
