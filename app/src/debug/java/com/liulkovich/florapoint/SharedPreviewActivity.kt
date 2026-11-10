
package com.liulkovich.florapoint

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.liulkovich.florapoint.presentation.screens.guide.GuideViewModel
import com.liulkovich.florapoint.presentation.screens.guide.toSharedGuideState
import com.liulkovich.florapoint.shared.ui.components.ForestPointBottomBar
import com.liulkovich.florapoint.shared.ui.guide.SharedGuideScreen
import com.liulkovich.florapoint.shared.ui.theme.ForestPointSharedTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.liulkovich.florapoint.presentation.screens.guide.toAndroidCommand
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.unit.dp

@AndroidEntryPoint
class SharedPreviewActivity : ComponentActivity() {

    private val guideViewModel: GuideViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val androidState by guideViewModel.state.collectAsState()
            val sharedState = androidState.toSharedGuideState()

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
                    Box(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        SharedGuideScreen(
                            state = sharedState,
                            onCommand = { command ->
                                guideViewModel.processCommand(
                                    command.toAndroidCommand()
                                )
                            },
                            onClickDetail = { speciesId ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Открыть вид ID: $speciesId"
                                    )
                                }
                            },

                            imageContent = { imageName, imageModifier ->

                                val context = LocalContext.current

                                val firstImageName = imageName
                                    .split(",")
                                    .map { it.trim() }
                                    .firstOrNull()
                                    ?: ""

                                val drawableId = context.resources.getIdentifier(
                                    firstImageName,
                                    "drawable",
                                    context.packageName
                                ).takeIf { it != 0 }
                                    ?: R.drawable.ic_launcher_background

                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(drawableId)
                                        .crossfade(true)
                                        .size(144)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = imageModifier
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }

                            )
                    }
                }
            }
        }
    }
}
