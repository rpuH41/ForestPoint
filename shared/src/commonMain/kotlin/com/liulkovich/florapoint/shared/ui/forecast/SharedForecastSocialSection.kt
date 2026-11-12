
package com.liulkovich.florapoint.shared.ui.forecast

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import org.jetbrains.compose.resources.stringResource

import com.liulkovich.florapoint.shared.domain.SharedForecastScreenState
import com.liulkovich.florapoint.shared.domain.SharedSocialForecastError
import com.liulkovich.florapoint.shared.generated.resources.*

@Composable
fun SharedForecastSocialSection(
    state: SharedForecastScreenState
) {
    when {
        state.socialLoading -> {
            Row(
                modifier = Modifier.padding(
                    horizontal = 8.dp,
                    vertical = 4.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = stringResource(
                        Res.string.forecast_analyzing_social_data
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        !state.socialMessage.isNullOrBlank() -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 8.dp,
                        vertical = 8.dp
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "💬",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = state.socialMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        state.socialError != null -> {
            val noInternet =
                state.socialError ==
                        SharedSocialForecastError.NO_INTERNET

            val errorTitle = if (noInternet) {
                stringResource(
                    Res.string.social_no_internet_title
                )
            } else {
                stringResource(
                    Res.string.social_service_error_title
                )
            }

            val errorMessage = if (noInternet) {
                stringResource(
                    Res.string.social_no_internet_message
                )
            } else {
                stringResource(
                    Res.string.social_service_error_message
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 8.dp,
                        vertical = 8.dp
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (noInternet) {
                            Icons.Default.WifiOff
                        } else {
                            Icons.Default.Warning
                        },
                        contentDescription = null,
                        tint =
                            MaterialTheme.colorScheme.onErrorContainer
                    )

                    Spacer(Modifier.width(8.dp))

                    Column {
                        Text(
                            text = errorTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color =
                                MaterialTheme.colorScheme.onErrorContainer
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}
