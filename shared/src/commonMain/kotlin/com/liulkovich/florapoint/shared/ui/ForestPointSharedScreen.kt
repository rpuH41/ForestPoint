
package com.liulkovich.florapoint.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ForestPointSharedScreen() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🌿 ForestPoint",
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    text = "Kotlin Multiplatform работает!",
                    modifier = Modifier.padding(top = 16.dp)
                )

                Text(
                    text = "Общий экран Android и iOS",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
