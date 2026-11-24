package com.liulkovich.florapoint.shared.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.liulkovich.florapoint.shared.domain.ReferenceModel
import com.liulkovich.florapoint.shared.domain.SharedFloraCategory
import com.liulkovich.florapoint.shared.domain.SharedUserPoint
import com.liulkovich.florapoint.shared.domain.localizedName
import com.liulkovich.florapoint.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** UI for a local map point. Publishing is added after iOS authentication/sync. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedAddMapPointSheet(
    species: List<ReferenceModel>,
    language: String,
    saveFailed: Boolean,
    onSave: (speciesId: Int?, name: String, description: String, category: String) -> Unit,
    onDismiss: () -> Unit,
    initialPoint: SharedUserPoint? = null,
) {
    var category by remember(initialPoint?.id) {
        mutableStateOf(
            if (initialPoint == null) SharedFloraCategory.MUSHROOM
            else SharedFloraCategory.fromKey(initialPoint.category.orEmpty())
                ?: SharedFloraCategory.OTHER
        )
    }
    var categoryExpanded by remember { mutableStateOf(false) }
    var name by remember(initialPoint?.id) {
        mutableStateOf(initialPoint?.userName.orEmpty())
    }
    var description by remember(initialPoint?.id) {
        mutableStateOf(initialPoint?.description.orEmpty())
    }
    var selectedSpeciesId by remember(initialPoint?.id) {
        mutableStateOf(initialPoint?.speciesId)
    }

    val categoryLabel = when (category) {
        SharedFloraCategory.MUSHROOM -> stringResource(Res.string.mushrooms)
        SharedFloraCategory.BERRY -> stringResource(Res.string.berries)
        SharedFloraCategory.PLANT -> stringResource(Res.string.plants)
        SharedFloraCategory.NUT -> stringResource(Res.string.nuts)
        SharedFloraCategory.OTHER -> stringResource(Res.string.category_other)
    }

    val suggestions = remember(species, category, name, language, selectedSpeciesId) {
        if (name.trim().length < 2 || selectedSpeciesId != null) emptyList()
        else species.filter {
            it.category == category.key &&
                    it.localizedName(language).contains(name.trim(), ignoreCase = true)
        }.take(5)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(
                if (initialPoint == null) Res.string.new_location
                else Res.string.map_edit_location
            ),
            style = MaterialTheme.typography.titleLarge,
        )

        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = it },
        ) {
            OutlinedTextField(
                value = categoryLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(Res.string.type)) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded)
                },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false },
            ) {
                SharedFloraCategory.entries.forEach { option ->
                    val label = when (option) {
                        SharedFloraCategory.MUSHROOM -> stringResource(Res.string.mushrooms)
                        SharedFloraCategory.BERRY -> stringResource(Res.string.berries)
                        SharedFloraCategory.PLANT -> stringResource(Res.string.plants)
                        SharedFloraCategory.NUT -> stringResource(Res.string.nuts)
                        SharedFloraCategory.OTHER -> stringResource(Res.string.category_other)
                    }
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            category = option
                            categoryExpanded = false
                            selectedSpeciesId = null
                            name = ""
                        },
                    )
                }
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                selectedSpeciesId = null
            },
            label = { Text(stringResource(Res.string.title)) },
            placeholder = { Text(stringResource(Res.string.enter_a_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        if (suggestions.isNotEmpty()) {
            Column {
                suggestions.forEach { candidate ->
                    TextButton(
                        onClick = {
                            name = candidate.localizedName(language)
                            selectedSpeciesId = candidate.id
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(candidate.localizedName(language))
                    }
                }
            }
        }

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(Res.string.description)) },
            placeholder = {
                Text(stringResource(Res.string.location_features_notes))
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
        )

        if (saveFailed) {
            Text(
                text = stringResource(Res.string.map_save_error),
                color = MaterialTheme.colorScheme.error,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.cancel))
            }
            Button(
                onClick = {
                    val resolvedId = selectedSpeciesId ?: species.firstOrNull {
                        it.category == category.key &&
                                it.localizedName(language).equals(name.trim(), ignoreCase = true)
                    }?.id
                    onSave(resolvedId, name.trim(), description.trim(), category.key)
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(Res.string.save))
            }
        }
    }
}
