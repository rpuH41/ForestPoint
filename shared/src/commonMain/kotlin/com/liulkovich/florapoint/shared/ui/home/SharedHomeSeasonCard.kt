
package com.liulkovich.florapoint.shared.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun SharedHomeSeasonCard(
    referenceId: Int,
    imageName: String,
    textName: String,
    daysLeftText: String,
    notificationEnabled: Boolean,
    onNotificationChange: (Boolean) -> Unit,
    onClickDetail: (Int) -> Unit,
    imageContent: @Composable (String, Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedImageName = remember(imageName) {
        imageName
            .split(",")
            .map { it.trim() }
            .randomOrNull()
            ?: ""
    }

    var selectedNotif by remember(referenceId) {
        mutableStateOf(notificationEnabled)
    }

    Card(
        modifier = modifier
            .width(165.dp)
            .height(150.dp),
        shape = RoundedCornerShape(15.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        onClick = {
            onClickDetail(referenceId)
        }
    ) {
        Column {
            Box {
                imageContent(
                    selectedImageName,
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )

                IconToggleButton(
                    modifier = Modifier.align(Alignment.TopEnd),
                    checked = selectedNotif,
                    onCheckedChange = { enabled ->
                        selectedNotif = enabled
                        onNotificationChange(enabled)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (selectedNotif) {
                            Color(0xFF4CAF50)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Text(
                modifier = Modifier.padding(
                    start = 8.dp,
                    top = 6.dp,
                    end = 8.dp
                ),
                text = textName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                modifier = Modifier.padding(
                    start = 8.dp,
                    end = 8.dp
                ),
                text = daysLeftText,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
