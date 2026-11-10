package com.liulkovich.florapoint.shared.ui.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.liulkovich.florapoint.shared.domain.SharedFloraCategory
import com.liulkovich.florapoint.shared.domain.localizedName

@Composable
fun GuideCategoryFilter(
    selectedCategories: Set<String>,
    onCategoryChange: (String) -> Unit,
    compactLabels: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(
            6.dp,
            androidx.compose.ui.Alignment.CenterHorizontally
        )
    ) {
        SharedFloraCategory.entries
            .filter { it != SharedFloraCategory.OTHER }
            .forEach { category ->
                val isSelected = category.key in selectedCategories

                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onCategoryChange(category.key)
                    },
                    label = {
                        if (compactLabels) {
                            Text(
                                text = category.emoji,
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "${category.emoji} ${category.localizedName("ru")}",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    modifier = Modifier.height(IntrinsicSize.Min)
                )
            }
    }
}
