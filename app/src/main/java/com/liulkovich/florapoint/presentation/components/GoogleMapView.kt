package com.liulkovich.florapoint.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.liulkovich.florapoint.domain.UserPoints
import androidx.compose.runtime.LaunchedEffect
import com.google.android.gms.maps.CameraUpdateFactory
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import com.liulkovich.florapoint.domain.Reference
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.platform.LocalContext
import com.liulkovich.florapoint.presentation.screens.map.utils.createShapeMarkerBitmap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.MapUiSettings
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import androidx.compose.runtime.DisposableEffect
import com.liulkovich.florapoint.presentation.screens.map.CurrentWeather
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.liulkovich.florapoint.R
import com.liulkovich.florapoint.presentation.screens.map.utils.isOnline
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size

@Composable
fun GoogleMapView(
    modifier: Modifier = Modifier,
    mapType: MapType = MapType.NORMAL,
    points: List<UserPoints> = emptyList(),
    currentLocation: Pair<Double, Double>? = null,
    shouldFollowLocation: Boolean = true,
    onLocationUpdate: (Pair<Double, Double>) -> Unit = {},
    zoomInRequest: Int = 0,
    selectedPointId: Int? = null,
    zoomOutRequest: Int = 0,
    currentWeather: CurrentWeather? = null,
    recenterRequest: Int = 0,
    species: List<Reference> = emptyList(),
    focusLocation: Pair<Double, Double>? = null,
    onMapClick: (LatLng) -> Unit = {},
    selectedMapLocation: Pair<Double, Double>? = null,
    onMarkerClick: (UserPoints) -> Unit = {}
) {
    val context = LocalContext.current
    val online by isOnline()
    val latestOnLocationUpdate by rememberUpdatedState(onLocationUpdate)


    val hasLocationPermission =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(53.133562, 25.141006),
            11f
        )
    }
    var liveGpsLocation by remember {
        mutableStateOf<Pair<Double, Double>?>(null)
    }

    DisposableEffect(context, hasLocationPermission) {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val listener = LocationListener { location ->
            val coordinates = location.latitude to location.longitude

            liveGpsLocation = coordinates
            latestOnLocationUpdate(coordinates)
        }

        if (hasLocationPermission) {
            try {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    0f,
                    listener
                )
            } catch (_: SecurityException) {
            } catch (_: IllegalArgumentException) {
            }
        }

        onDispose {
            locationManager.removeUpdates(listener)
        }
    }

    var hasCenteredOnLocation by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(liveGpsLocation, currentLocation, shouldFollowLocation) {
        val location = liveGpsLocation ?: currentLocation

        if (location != null &&
            (shouldFollowLocation || !hasCenteredOnLocation)
        ) {
            cameraPositionState.move(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(location.first, location.second),
                    if (hasCenteredOnLocation) {
                        cameraPositionState.position.zoom
                    } else {
                        11f
                    }
                )
            )

            hasCenteredOnLocation = true
        }
    }


    LaunchedEffect(recenterRequest) {
        if (recenterRequest > 0) {
            val location = liveGpsLocation ?: currentLocation

            if (location != null) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            location.first,
                            location.second
                        ),
                        11f
                    )
                )
            }
        }
    }


    LaunchedEffect(zoomInRequest) {
        if (zoomInRequest > 0) {
            cameraPositionState.animate(
                CameraUpdateFactory.zoomIn()
            )
        }
    }

    LaunchedEffect(zoomOutRequest) {
        if (zoomOutRequest > 0) {
            cameraPositionState.animate(
                CameraUpdateFactory.zoomOut()
            )
        }
    }

    LaunchedEffect(focusLocation) {
        if (focusLocation != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLng(
                    LatLng(
                        focusLocation.first,
                        focusLocation.second
                    )
                )
            )
        }
    }
    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = mapType,
                isMyLocationEnabled = hasLocationPermission
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false
            ),
            onMapClick = { location ->
                onMapClick(location)
            }
        ) {

            points.forEach { point ->

                val ref = species.find { it.id == point.speciesId }

                val category = when {
                    !point.category.isNullOrBlank() ->
                        point.category.lowercase().trim()

                    ref != null && ref.category.isNotBlank() ->
                        ref.category.lowercase().trim()

                    else -> "custom"
                }

                val markerState = remember(
                    point.id,
                    point.latitude,
                    point.longitude
                ) {
                    MarkerState(
                        position = LatLng(point.latitude, point.longitude)
                    )
                }

                val markerIcon = remember(category) {
                    BitmapDescriptorFactory.fromBitmap(
                        createShapeMarkerBitmap(category)
                    )
                }

                Marker(
                    state = markerState,
                    icon = markerIcon,
                    title = point.userName,
                    alpha = if (point.id == selectedPointId) 1f else 0.85f,
                    onClick = {
                        onMarkerClick(point)
                        true
                    }
                )
            }


            selectedMapLocation?.let { (latitude, longitude) ->

                val selectionMarkerState = remember(latitude, longitude) {
                    MarkerState(
                        position = LatLng(latitude, longitude)
                    )
                }

                val selectionIcon = remember(context) {
                    val drawable = createSelectionMarkerBitmap(context) as BitmapDrawable
                    BitmapDescriptorFactory.fromBitmap(drawable.bitmap)
                }

                Marker(
                    state = selectionMarkerState,
                    icon = selectionIcon,
                    title = "Выбранное место",
                    onClick = { true }
                )
            }

        }


        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!online) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xCC000000)
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )

                        Text(
                            text = stringResource(R.string.offline),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            if (currentWeather != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xCC000000)
                ) {
                    FlowRow(
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.weather_now),
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "☀️ ${currentWeather.temperature.toInt()}°",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "💧 ${currentWeather.humidity}%",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.weather_last_5days),
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "🌡️ ${currentWeather.avgTemp5Days.toInt()}°",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "💧 ${currentWeather.avgHumidity5Days}%",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}
