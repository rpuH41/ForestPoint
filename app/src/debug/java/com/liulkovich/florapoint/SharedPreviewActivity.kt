
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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.liulkovich.florapoint.shared.ui.components.ForestPointBottomBar
import com.liulkovich.florapoint.shared.ui.theme.ForestPointSharedTheme
import kotlinx.coroutines.launch

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


            val snackbarHostState = remember {
                SnackbarHostState()
            }

            val scope = rememberCoroutineScope()

            ForestPointSharedTheme(
                colorScheme = androidColorScheme
            ) {
                Scaffold(
                    snackbarHost = {
                        SnackbarHost(snackbarHostState)
                    },
                    bottomBar = {
                        ForestPointBottomBar(
                            currentRoute = "Guide",
                            onNavigate = { route ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Тест перехода: $route"
                                    )
                                }
                            },
                            onForecastClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Тест кнопки прогноза 🍄"
                                    )
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        ForestPointSharedScreen(
                            species = species,
                            colorScheme = androidColorScheme
                        )
                    }
                }
            }

        }
    }
}
