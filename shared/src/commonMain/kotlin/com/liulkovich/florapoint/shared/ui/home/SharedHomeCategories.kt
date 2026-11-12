
package com.liulkovich.florapoint.shared.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import org.jetbrains.compose.resources.stringResource

import com.liulkovich.florapoint.shared.domain.SharedFloraCategory
import com.liulkovich.florapoint.shared.generated.resources.*
import androidx.compose.ui.unit.sp

@Composable
fun SharedHomeCategories(
    onClickCategory: (String) -> Unit,
    imageContent: @Composable (String, Modifier) -> Unit
) {
    val categories = SharedFloraCategory.entries
        .filter { it != SharedFloraCategory.OTHER }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        categories.chunked(2).forEach { categoryRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                categoryRow.forEach { category ->
                    val categoryName = when (category) {
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

                    SharedHomeCategoryCard(
                        modifier = Modifier.weight(1f),
                        name = categoryName,
                        imageKey = category.key,
                        onClick = {
                            onClickCategory(category.key)
                        },
                        imageContent = imageContent
                    )
                }
            }
        }
    }
}

@Composable
private fun SharedHomeCategoryCard(
    modifier: Modifier,
    name: String,
    imageKey: String,
    onClick: () -> Unit,
    imageContent: @Composable (String, Modifier) -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        onClick = onClick
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            imageContent(
                imageKey,
                Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .padding(8.dp)
            )

            Text(
                modifier = Modifier.padding(vertical = 8.dp),
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
@Composable
fun SharedHomeReferenceTitle() {
    Text(
        text = stringResource(Res.string.reference),
        style = MaterialTheme.typography.titleSmall,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(
            start = 16.dp,
            top = 8.dp,
            bottom = 4.dp
        )
    )
}