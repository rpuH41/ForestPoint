
package com.liulkovich.florapoint.shared.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.liulkovich.florapoint.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource

@Composable
fun SharedSpeciesImage(
    imageName: String,
    modifier: Modifier = Modifier
) {
    // В базе могут быть перечислены несколько фотографий:
    // "image_one,image_two".
    val imageResource = imageName
        .split(",")
        .map { it.trim().substringBeforeLast(".").lowercase() }
        .firstNotNullOfOrNull { name ->
            Res.allDrawableResources[name]
        }

    if (imageResource != null) {
        Image(
            painter = painterResource(imageResource),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text("🌿")
        }
    }
}
