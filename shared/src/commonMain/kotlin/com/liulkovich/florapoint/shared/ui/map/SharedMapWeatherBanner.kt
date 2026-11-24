
package com.liulkovich.florapoint.shared.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.liulkovich.florapoint.shared.domain.SharedWeatherData
import com.liulkovich.florapoint.shared.generated.resources.Res
import com.liulkovich.florapoint.shared.generated.resources.weather_now
import com.liulkovich.florapoint.shared.generated.resources.weather_last_5days
import org.jetbrains.compose.resources.stringResource

@Composable
fun SharedMapWeatherBanner(
    weather: SharedWeatherData,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xCC000000)
    ) {
        FlowRow(
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 4.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.weather_now),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall
                )

                Text(
                    text = "☀️ ${weather.temperature.toInt()}°",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )

                Text(
                    text = "💧 ${weather.humidity}%",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        Res.string.weather_last_5days
                    ),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall
                )

                Text(
                    text = "🌡️ ${weather.avgTemp5Days.toInt()}°",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )

                Text(
                    text = "💧 ${weather.avgHumidity5Days}%",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
