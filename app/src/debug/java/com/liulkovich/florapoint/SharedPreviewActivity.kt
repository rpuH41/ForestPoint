
package com.liulkovich.florapoint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.liulkovich.florapoint.data.ReferenceDao
import com.liulkovich.florapoint.domain.toReferenceModel
import com.liulkovich.florapoint.shared.ui.ForestPointSharedScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalContext

@AndroidEntryPoint
class SharedPreviewActivity : ComponentActivity() {

    @Inject
    lateinit var referenceDao: ReferenceDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val speciesFlow = referenceDao.getAllSpecies()
            .map { references ->
                references.map { reference ->
                    reference.toReferenceModel()
                }
            }

        setContent {
            val species by speciesFlow.collectAsState(
                initial = emptyList()
            )

            val context = LocalContext.current
            val darkTheme = isSystemInDarkTheme()

            val androidColorScheme =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (darkTheme) {
                        dynamicDarkColorScheme(context)
                    } else {
                        dynamicLightColorScheme(context)
                    }
                } else {
                    null
                }

            ForestPointSharedScreen(
                species = species,
                colorScheme = androidColorScheme
            )
        }
    }
}
