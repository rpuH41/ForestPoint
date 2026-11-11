
package com.liulkovich.florapoint.shared.ui.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import org.jetbrains.compose.resources.stringResource
import com.liulkovich.florapoint.shared.generated.resources.*

@Composable
fun SharedMapActionButtons(
    isAuthorized: Boolean,
    showOnlyMyPoints: Boolean,
    onAddPoint: () -> Unit,
    onToggleFilter: () -> Unit,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false
) {
    val buttonHeight = if (isLandscape) 28.dp else 36.dp
    val fontSize = if (isLandscape) 11.sp else 13.sp
    val iconSize = if (isLandscape) 12.dp else 16.dp

    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopEnd
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .border(
                    width = 1.dp,
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(
                    horizontal = 12.dp,
                    vertical = 15.dp
                )
        ) {
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAddPoint,
                    modifier = Modifier.height(buttonHeight),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1B5E20),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(
                        horizontal = if (isLandscape) 8.dp else 12.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AddLocationAlt,
                        contentDescription = null,
                        modifier = Modifier.size(iconSize)
                    )

                    Spacer(Modifier.width(4.dp))

                    Text(
                        text = stringResource(Res.string.add_place),
                        fontSize = fontSize
                    )
                }

                if (isAuthorized) {
                    Spacer(
                        Modifier.width(
                            if (isLandscape) 4.dp else 8.dp
                        )
                    )

                    Button(
                        onClick = onToggleFilter,
                        modifier = Modifier.height(buttonHeight),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showOnlyMyPoints) {
                                Color(0xFF1B5E20)
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                            contentColor = if (showOnlyMyPoints) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        ),
                        contentPadding = PaddingValues(
                            horizontal = if (isLandscape) 8.dp else 12.dp
                        )
                    ) {
                        Text(
                            text = if (showOnlyMyPoints) {
                                stringResource(Res.string.filter_my_points)
                            } else {
                                stringResource(Res.string.filter_all_points)
                            },
                            fontSize = fontSize
                        )
                    }
                }
            }
        }

        Text(
            text = stringResource(Res.string.place),
            fontSize = 12.sp,
            lineHeight = 12.sp,
            color = Color.Black,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp)
                .background(
                    color = Color.White.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 2.dp)
        )
    }
}
