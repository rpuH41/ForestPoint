package com.liulkovich.florapoint.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.liulkovich.florapoint.shared.domain.SharedFloraCategory
import com.liulkovich.florapoint.shared.domain.localizedName
import com.liulkovich.florapoint.shared.domain.ReferenceModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.liulkovich.florapoint.shared.domain.localizedName
import com.liulkovich.florapoint.shared.ui.theme.ForestPointSharedTheme
import androidx.compose.material3.ColorScheme


@Composable
fun ForestPointSharedScreen(
    species: List<ReferenceModel> = emptyList(),
    colorScheme: ColorScheme? = null
) {

    var selectedCategory by remember {
        mutableStateOf<SharedFloraCategory?>(null)
    }

    ForestPointSharedTheme(colorScheme = colorScheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "🌿 ForestPoint",
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    text = "Общий справочник Android и iOS",
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )


                if (selectedCategory == null) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(SharedFloraCategory.entries.toList()) { category ->
                            val speciesCount = species.count {
                                it.category == category.key
                            }

                            Card(
                                onClick = {
                                    selectedCategory = category
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = category.emoji,
                                        style = MaterialTheme.typography.headlineSmall
                                    )

                                    Text(
                                        text = category.localizedName("ru"),
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Text(
                                        text = "В базе: $speciesCount",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val category = selectedCategory!!

                    val filteredSpecies = species.filter {
                        it.category == category.key
                    }

                    TextButton(
                        onClick = {
                            selectedCategory = null
                        }
                    ) {
                        Text("← Назад к категориям")
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredSpecies) { reference ->
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = reference.localizedName("ru"),
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }

            }
        }
    }
}
