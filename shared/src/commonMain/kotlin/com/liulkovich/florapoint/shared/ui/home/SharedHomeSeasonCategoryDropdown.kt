package com.liulkovich.florapoint.shared.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

import org.jetbrains.compose.resources.stringResource

import com.liulkovich.florapoint.shared.domain.SharedFloraCategory
import com.liulkovich.florapoint.shared.generated.resources.*

@Composable
fun SharedHomeSeasonCategoryDropdown(
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    val categories = SharedFloraCategory.entries
        .filter { it != SharedFloraCategory.OTHER }

    val currentCategory = categories.find {
        it.key == selectedCategory
    }

    val currentLabel = if (currentCategory == null) {
        stringResource(Res.string.all_seasons)
    } else {
        sharedHomeCategoryName(currentCategory)
    }

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(
                    horizontal = 8.dp,
                    vertical = 4.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(Res.string.all_seasons)
                    )
                },
                onClick = {
                    onCategorySelected(null)
                    expanded = false
                }
            )

            categories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Text(
                            "${category.emoji} " +
                                    sharedHomeCategoryName(category)
                        )
                    },
                    onClick = {
                        onCategorySelected(category.key)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun sharedHomeCategoryName(
    category: SharedFloraCategory
): String {
    return when (category) {
        SharedFloraCategory.MUSHROOM ->
            stringResource(Res.string.mushrooms)

        SharedFloraCategory.BERRY ->
            stringResource(Res.string.berries)

        SharedFloraCategory.PLANT ->
            stringResource(Res.string.plants)

        SharedFloraCategory.NUT ->
            stringResource(Res.string.nuts)

        SharedFloraCategory.OTHER -> ""
    }
}
