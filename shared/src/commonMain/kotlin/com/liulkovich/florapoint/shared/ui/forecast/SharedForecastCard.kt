
package com.liulkovich.florapoint.shared.ui.forecast

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import org.jetbrains.compose.resources.stringResource

import com.liulkovich.florapoint.shared.domain.SharedForecastItem
import com.liulkovich.florapoint.shared.domain.localizedName
import com.liulkovich.florapoint.shared.generated.resources.*

@Composable
fun SharedForecastCard(
    item: SharedForecastItem,
    language: String,
    onClickDetail: (Int) -> Unit,
    onLocationClick: (Int?) -> Unit,
    formatDistance: (Double) -> String,
    modifier: Modifier = Modifier
) {
    val scoreColor = when {
        item.score >= 80 -> Color(0xFF1BA226)
        item.score >= 50 -> Color(0xFFF9A825)
        else -> Color(0xFFE65100)
    }

    val scoreText = when {
        item.score >= 80 ->
            stringResource(Res.string.forecast_excellent)

        item.score >= 50 ->
            stringResource(Res.string.forecast_good)

        else ->
            stringResource(Res.string.forecast_weak)
    }

    Card(
        onClick = {
            onClickDetail(item.reference.id)
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .width(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🍄",
                    style = MaterialTheme.typography.headlineMedium,
                    color = scoreColor
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.reference.localizedName(language),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = scoreText,
                    style = MaterialTheme.typography.bodySmall,
                    color = scoreColor,
                    fontWeight = FontWeight.Medium
                )
            }

            if (item.hasNearbyPoint) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = {
                            onLocationClick(item.nearbyPointId)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    item.nearbyDistanceKm?.let { distance ->
                        Text(
                            text = formatDistance(distance),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
    }
}
