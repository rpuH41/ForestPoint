
package com.liulkovich.florapoint

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

import com.liulkovich.florapoint.domain.FloraCategory

@Composable
fun SharedHomeCategoryImage(
    imageKey: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val imageName = FloraCategory.entries
        .firstOrNull { it.key == imageKey }
        ?.imageName

    val imageId = remember(imageKey, imageName) {
        if (imageName.isNullOrBlank()) {
            0
        } else {
            context.resources.getIdentifier(
                imageName,
                "drawable",
                context.packageName
            )
        }
    }

    Image(
        painter = painterResource(
            id = imageId.takeIf { it != 0 }
                ?: R.drawable.ic_launcher_background
        ),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}
