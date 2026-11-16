
package com.liulkovich.florapoint

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.viewinterop.AndroidView

import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.google.android.gms.location.LocationServices
import com.liulkovich.florapoint.presentation.screens.settings.SettingsViewModel
import com.liulkovich.florapoint.shared.domain.SharedDownloadAreaEstimate
import com.liulkovich.florapoint.shared.domain.SharedDownloadAreaProgress
import com.liulkovich.florapoint.shared.domain.SharedDownloadAreaState
import com.liulkovich.florapoint.shared.ui.settings.SharedDownloadAreaScreen

import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

import kotlin.math.abs
import kotlin.math.cos

@Composable
fun SharedDownloadAreaPreview(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val progress by viewModel.downloadProgress
        .collectAsStateWithLifecycle()

    var isDownloading by remember {
        mutableStateOf(false)
    }

    var selectionRect by remember {
        mutableStateOf<IntRect?>(null)
    }

    var selectedArea by remember {
        mutableStateOf<BoundingBox?>(null)
    }

    // Используем одну карту на всё время работы экрана.
    val mapView = remember(context) {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)

            controller.setZoom(13.0)
            controller.setCenter(
                GeoPoint(53.133562, 25.141006)
            )
        }
    }

    DisposableEffect(mapView) {
        onDispose {
            mapView.onDetach()
        }
    }

    fun centerOnCurrentLocation() {
        val hasFinePermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val hasCoarsePermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!hasFinePermission && !hasCoarsePermission) {
            Toast.makeText(
                context,
                "Нет разрешения на определение местоположения",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val locationClient =
            LocationServices.getFusedLocationProviderClient(context)

        locationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    mapView.controller.animateTo(
                        GeoPoint(
                            location.latitude,
                            location.longitude
                        )
                    )
                }
            }
    }

    // Центрируем карту при первом открытии.
    LaunchedEffect(mapView) {
        centerOnCurrentLocation()
    }

    // Преобразуем зелёную рамку в реальные
    // географические координаты.
    fun getSelectedBoundingBox(): BoundingBox? {
        val rect = selectionRect ?: return null

        val width = mapView.width
        val height = mapView.height

        if (width < 2 || height < 2) return null

        val left = rect.left.coerceIn(0, width - 1)
        val top = rect.top.coerceIn(0, height - 1)

        val right = rect.right.coerceIn(0, width - 1)
        val bottom = rect.bottom.coerceIn(0, height - 1)

        if (right <= left || bottom <= top) {
            return null
        }

        val projection = mapView.projection

        val topLeft = projection.fromPixels(left, top)
        val bottomRight = projection.fromPixels(right, bottom)

        return BoundingBox(
            topLeft.latitude,
            bottomRight.longitude,
            bottomRight.latitude,
            topLeft.longitude
        )
    }

    // Когда скачивание завершилось или произошла ошибка,
    // возвращаем кнопку в обычное состояние.
    LaunchedEffect(
        progress?.isFinished,
        progress?.isError
    ) {
        if (
            progress?.isFinished == true ||
            progress?.isError == true
        ) {
            isDownloading = false
        }
    }

    val sharedProgress = progress?.let {
        SharedDownloadAreaProgress(
            current = it.current,
            total = it.total,
            isFinished = it.isFinished,
            isError = it.isError
        )
    }

    val sharedState = SharedDownloadAreaState(
        isDownloading = isDownloading,
        progress = sharedProgress
    )

    SharedDownloadAreaScreen(
        modifier = modifier,
        state = sharedState,

        mapContent = { mapModifier ->
            AndroidView(
                modifier = mapModifier,
                factory = {
                    mapView
                }
            )
        },

        onSelectionBoundsChanged = { rect ->
            selectionRect = rect
        },

        onZoomIn = {
            mapView.controller.zoomIn()
        },

        onZoomOut = {
            mapView.controller.zoomOut()
        },

        onRecenter = {
            centerOnCurrentLocation()
        },

        onEstimateRequested = {
            val bounds = getSelectedBoundingBox()

            selectedArea = bounds

            if (bounds == null) {
                SharedDownloadAreaEstimate()
            } else {
                val centerLat =
                    (bounds.latNorth + bounds.latSouth) / 2.0

                val latKm =
                    abs(bounds.latNorth - bounds.latSouth) *
                            111.32

                val lonKm =
                    abs(bounds.lonEast - bounds.lonWest) *
                            111.32 *
                            cos(Math.toRadians(centerLat))

                val areaKm2 = latKm * lonKm

                val tilesCount = viewModel.countTiles(bounds)

                val approximateSizeMb =
                    tilesCount * 15_000.0 /
                            (1024.0 * 1024.0)

                SharedDownloadAreaEstimate(
                    areaKm2 = areaKm2,
                    tilesCount = tilesCount,
                    approximateSizeMb = approximateSizeMb
                )
            }
        },

        onDownloadRequested = { regionName ->
            val bounds = selectedArea

            if (bounds == null) {
                Toast.makeText(
                    context,
                    "Не удалось определить область загрузки",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                viewModel.resetDownloadProgress()

                isDownloading = true

                viewModel.downloadArea(
                    context = context,
                    boundingBox = bounds,
                    regionName = regionName
                )
            }
        }
    )
}
