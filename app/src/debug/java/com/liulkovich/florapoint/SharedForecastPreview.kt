
package com.liulkovich.florapoint

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

import com.liulkovich.florapoint.domain.LocationUtils
import com.liulkovich.florapoint.presentation.screens.forecast.ForecastViewModel
import com.liulkovich.florapoint.presentation.screens.map.utils.isOnline
import com.liulkovich.florapoint.presentation.weather.WeatherViewModel
import com.liulkovich.florapoint.shared.ui.forecast.SharedForecastScreen

@Composable
fun SharedForecastPreview(
    language: String,
    onClickDetail: (Int) -> Unit,
    onLocationClick: (Int?) -> Unit
) {
    val context = LocalContext.current

    val forecastViewModel: ForecastViewModel = hiltViewModel()
    val weatherViewModel: WeatherViewModel = hiltViewModel()

    val forecastState by forecastViewModel.state.collectAsState()
    val weather by weatherViewModel.currentWeather.collectAsState()
    val isWeatherLoading by weatherViewModel.isLoadingWeather.collectAsState()
    val location by weatherViewModel.currentLocation.collectAsState()

    val online by isOnline()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            requestForecastLocation(
                context = context,
                weatherViewModel = weatherViewModel,
                onUnavailable = {
                    forecastViewModel.load(
                        weather = null,
                        isWeatherLoading = false,
                        currentLat = 0.0,
                        currentLon = 0.0
                    )
                }
            )
        } else {
            forecastViewModel.load(
                weather = null,
                isWeatherLoading = false,
                currentLat = 0.0,
                currentLon = 0.0
            )
        }
    }

    // Запрашиваем координаты при открытии экрана.
    LaunchedEffect(Unit) {
        val hasFinePermission =
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val hasCoarsePermission =
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (hasFinePermission || hasCoarsePermission) {
            requestForecastLocation(
                context = context,
                weatherViewModel = weatherViewModel,
                onUnavailable = {
                    forecastViewModel.load(
                        weather = null,
                        isWeatherLoading = false,
                        currentLat = 0.0,
                        currentLon = 0.0
                    )
                }
            )
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Повторяем условия загрузки оригинального ForecastScreen.
    LaunchedEffect(
        weather,
        isWeatherLoading,
        location,
        online
    ) {
        when {
            !online -> {
                forecastViewModel.load(
                    weather = null,
                    isWeatherLoading = false,
                    currentLat = 0.0,
                    currentLon = 0.0
                )
            }

            location != null -> {
                forecastViewModel.load(
                    weather = weather,
                    isWeatherLoading = isWeatherLoading,
                    currentLat = location!!.first,
                    currentLon = location!!.second
                )
            }
        }
    }

    SharedForecastScreen(
        state = forecastState.toSharedForecastScreenState(),
        language = language,
        onClickDetail = onClickDetail,
        onLocationClick = onLocationClick,
        formatDistance = { distance ->
            LocationUtils.formatDistance(distance)
        }
    )
}

@SuppressLint("MissingPermission")
private fun requestForecastLocation(
    context: Context,
    weatherViewModel: WeatherViewModel,
    onUnavailable: () -> Unit
) {
    val hasFinePermission =
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    val hasCoarsePermission =
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    if (!hasFinePermission && !hasCoarsePermission) {
        onUnavailable()
        return
    }

    val client =
        LocationServices.getFusedLocationProviderClient(context)

    client.getCurrentLocation(
        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
        null
    ).addOnSuccessListener { currentLocation ->

        if (currentLocation != null) {
            weatherViewModel.updateLocation(
                currentLocation.latitude,
                currentLocation.longitude
            )
        } else {
            client.lastLocation
                .addOnSuccessListener { lastLocation ->
                    if (lastLocation != null) {
                        weatherViewModel.updateLocation(
                            lastLocation.latitude,
                            lastLocation.longitude
                        )
                    } else {
                        onUnavailable()
                    }
                }
                .addOnFailureListener {
                    onUnavailable()
                }
        }
    }.addOnFailureListener {
        onUnavailable()
    }
}
