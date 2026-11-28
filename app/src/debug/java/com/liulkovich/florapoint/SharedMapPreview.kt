
package com.liulkovich.florapoint

import android.content.res.Configuration
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager

import androidx.core.content.ContextCompat
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.background
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme

import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.collectLatest
import com.liulkovich.florapoint.presentation.screens.map.MapCommand
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.maps.android.compose.MapType
import com.liulkovich.florapoint.presentation.components.GoogleMapView
import com.liulkovich.florapoint.presentation.components.OsmMapView
import com.liulkovich.florapoint.presentation.screens.map.GoogleMapLayer
import com.liulkovich.florapoint.presentation.screens.map.MapProvider
import com.liulkovich.florapoint.presentation.screens.map.MapViewModel
import com.liulkovich.florapoint.shared.ui.map.SharedMapActionButtons
import com.liulkovich.florapoint.shared.ui.map.SharedMapLayerMenu
import com.liulkovich.florapoint.shared.ui.map.SharedMapNavigationControls
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun SharedMapPreview(
    modifier: Modifier = Modifier,
    mapViewModel: MapViewModel = hiltViewModel()
) {
    val mapState by mapViewModel.state.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val hasLocationPermission =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

    var locationRequestCompleted by remember {
        mutableStateOf(false)
    }

    var mapVisible by remember {
        mutableStateOf(false)
    }

    DisposableEffect(context, mapViewModel, hasLocationPermission) {
        var active = true

        if (!hasLocationPermission ||
            mapViewModel.state.value.currentUserLocation != null
        ) {
            locationRequestCompleted = true
        } else {
            val fusedClient =
                LocationServices.getFusedLocationProviderClient(context)

            fun acceptLocation(location: android.location.Location?) {
                if (!active) return

                if (location != null) {
                    mapViewModel.updateCurrentLocation(
                        location.latitude,
                        location.longitude
                    )
                }

                locationRequestCompleted = true
            }

            try {
                fusedClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (!active) return@addOnSuccessListener

                        if (location != null) {
                            acceptLocation(location)
                        } else {
                            try {
                                fusedClient.getCurrentLocation(
                                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                                    null
                                )
                                    .addOnSuccessListener { freshLocation ->
                                        acceptLocation(freshLocation)
                                    }
                                    .addOnFailureListener {
                                        if (active) {
                                            locationRequestCompleted = true
                                        }
                                    }
                            } catch (_: SecurityException) {
                                if (active) {
                                    locationRequestCompleted = true
                                }
                            }
                        }
                    }
                    .addOnFailureListener {
                        if (active) {
                            locationRequestCompleted = true
                        }
                    }
            } catch (_: SecurityException) {
                locationRequestCompleted = true
            }
        }

        onDispose {
            active = false
        }
    }

// После получения координат даём картографическому
// компоненту два кадра на центрирование камеры.
    LaunchedEffect(
        mapState.currentUserLocation,
        locationRequestCompleted,
        hasLocationPermission
    ) {
        if (
            !hasLocationPermission ||
            mapState.currentUserLocation != null ||
            locationRequestCompleted
        ) {
            withFrameNanos { }
            withFrameNanos { }
            mapVisible = true
        }
    }

    val configuration = LocalConfiguration.current

    val isLandscape =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val osmMapRef = remember {
        mutableStateOf<MapView?>(null)
    }

    val osmLocationOverlayRef = remember {
        mutableStateOf<MyLocationNewOverlay?>(null)
    }

    var googleGpsLocation by remember {
        mutableStateOf<Pair<Double, Double>?>(null)
    }

    var googleZoomInRequest by remember {
        mutableIntStateOf(0)
    }

    var googleZoomOutRequest by remember {
        mutableIntStateOf(0)
    }

    var googleRecenterRequest by remember {
        mutableIntStateOf(0)
    }

    var shouldFollowLocation by remember {
        mutableStateOf(true)
    }

    var googleFocusLocation by remember {
        mutableStateOf<Pair<Double, Double>?>(null)
    }

    val mapPoints = mapState.sortedPointsWithDistance.map {
        it.first
    }


    LaunchedEffect(mapViewModel) {
        mapViewModel.command.collectLatest { command ->
            if (command is MapCommand.CenterMapOnPoint) {
                val target = command.point.latitude to
                        command.point.longitude

                // Отключаем автоматическое следование за GPS,
                // чтобы карта оставалась на выбранной точке.
                shouldFollowLocation = false

                // Сбрасываем предыдущую цель Google Maps.
                // Это позволяет снова перейти к той же точке.
                googleFocusLocation = null

                // Даём Compose применить изменение состояния.
                withFrameNanos { }

                // OpenStreetMap.
                osmMapRef.value?.controller?.animateTo(
                    GeoPoint(target.first, target.second)
                )

                // Google Maps.
                googleFocusLocation = target
            }
        }
    }


    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when (mapState.mapProvider) {

            MapProvider.OSM -> {
                OsmMapView(
                    modifier = Modifier.fillMaxSize(),

                    points = mapPoints,
                    species = mapState.species,
                    shouldFollowLocation = shouldFollowLocation,
                    currentLocation = mapState.currentUserLocation,
                    selectedPointId = mapState.selectedPointId,
                    currentWeather = mapState.currentWeather,

                    onMapReady = { mapView, locationOverlay ->
                        mapView.setBuiltInZoomControls(false)
                        osmMapRef.value = mapView
                        osmLocationOverlayRef.value = locationOverlay
                    },

                    onMarkerClick = { point ->
                        if (mapViewModel.isPublicForeignPoint(point)) {
                            mapViewModel.onPublicPointClicked(point)
                        } else {
                            mapViewModel.onPointClicked(point)
                        }
                    },

                    onMapClick = { geoPoint ->
                        mapViewModel.onAddNewPointClicked(
                            geoPoint.latitude,
                            geoPoint.longitude
                        )
                    }
                )
            }

            MapProvider.GOOGLE -> {
                val mapType = when (mapState.googleMapLayer) {
                    GoogleMapLayer.NORMAL -> MapType.NORMAL
                    GoogleMapLayer.SATELLITE -> MapType.SATELLITE
                    GoogleMapLayer.HYBRID -> MapType.HYBRID
                    GoogleMapLayer.TERRAIN -> MapType.TERRAIN
                }

                GoogleMapView(
                    modifier = Modifier.fillMaxSize(),

                    mapType = mapType,
                    points = mapPoints,
                    species = mapState.species,

                    zoomInRequest = googleZoomInRequest,
                    zoomOutRequest = googleZoomOutRequest,
                    recenterRequest = googleRecenterRequest,
                    shouldFollowLocation = shouldFollowLocation,
                    focusLocation = googleFocusLocation,
                    currentLocation = mapState.currentUserLocation,
                    currentWeather = mapState.currentWeather,
                    selectedPointId = mapState.selectedPointId,

                    onLocationUpdate = { coordinates ->
                        googleGpsLocation = coordinates

                        mapViewModel.updateCurrentLocation(
                            coordinates.first,
                            coordinates.second
                        )
                    },

                    onMarkerClick = { point ->
                        if (mapViewModel.isPublicForeignPoint(point)) {
                            mapViewModel.onPublicPointClicked(point)
                        } else {
                            mapViewModel.onPointClicked(point)
                        }
                    },

                    onMapClick = { location ->
                        mapViewModel.onAddNewPointClicked(
                            location.latitude,
                            location.longitude
                        )
                    }
                )
            }
        }

        SharedMapLayerMenu(
            modifier = Modifier
                .align(
                    if (isLandscape) {
                        Alignment.TopStart
                    } else {
                        Alignment.TopEnd
                    }
                )
                .padding(16.dp),

            settings = mapState.toSharedMapSettings(),
            isLandscape = isLandscape,

            onProviderSelected = { provider ->
                mapViewModel.setMapProvider(
                    provider.toAndroidMapProvider()
                )
            },

            onGoogleLayerSelected = { layer ->
                mapViewModel.setGoogleMapLayer(
                    layer.toAndroidGoogleMapLayer()
                )
            }
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    if (isLandscape) 8.dp else 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(
                if (isLandscape) 4.dp else 8.dp
            ),
            horizontalAlignment = Alignment.End
        ) {
            SharedMapNavigationControls(
                onMyLocation = {
                    shouldFollowLocation = true
                    googleFocusLocation = null

                    if (mapState.mapProvider == MapProvider.GOOGLE) {
                        googleRecenterRequest++
                    } else {
                        val location =
                            osmLocationOverlayRef.value?.myLocation

                        if (location != null) {
                            osmMapRef.value?.controller?.animateTo(
                                location
                            )
                        } else {
                            mapState.currentUserLocation?.let { (lat, lon) ->
                                osmMapRef.value?.controller?.animateTo(
                                    GeoPoint(lat, lon)
                                )
                            }
                        }
                    }
                },
                isLandscape = isLandscape,

                onZoomIn = {
                    if (mapState.mapProvider == MapProvider.GOOGLE) {
                        googleZoomInRequest++
                    } else {
                        osmMapRef.value?.controller?.zoomIn()
                    }
                },

                onZoomOut = {
                    if (mapState.mapProvider == MapProvider.GOOGLE) {
                        googleZoomOutRequest++
                    } else {
                        osmMapRef.value?.controller?.zoomOut()
                    }
                }
            )

            SharedMapActionButtons(

                isLandscape = isLandscape,
                isAuthorized = mapViewModel.isAuthorized(),
                showOnlyMyPoints = mapState.showOnlyMyPoints,

                onAddPoint = {
                    val location = when (mapState.mapProvider) {
                        MapProvider.GOOGLE ->
                            googleGpsLocation
                                ?: mapState.currentUserLocation

                        MapProvider.OSM ->
                            osmLocationOverlayRef.value
                                ?.myLocation
                                ?.let { it.latitude to it.longitude }
                                ?: mapState.currentUserLocation
                    }

                    if (location != null) {
                        mapViewModel.onAddNewPointClicked(
                            location.first,
                            location.second
                        )
                    } else {
                        val message =
                            if (configuration.locales[0]?.language == "ru") {
                                "Геолокация недоступна. " +
                                        "Нажмите на нужное место на карте."
                            } else {
                                "Location unavailable. " +
                                        "Tap a place on the map."
                            }

                        Toast.makeText(
                            context,
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },

                onToggleFilter = {
                    mapViewModel.togglePointsFilter()
                }
            )
        }
        if (!mapVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
