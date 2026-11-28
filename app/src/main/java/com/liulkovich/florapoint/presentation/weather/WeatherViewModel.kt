package com.liulkovich.florapoint.presentation.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liulkovich.florapoint.data.weather.WeatherService
import com.liulkovich.florapoint.presentation.screens.map.CurrentWeather
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherService: WeatherService
) : ViewModel() {

    private val _currentLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val currentLocation = _currentLocation.asStateFlow()

    private val _currentWeather = MutableStateFlow<CurrentWeather?>(null)
    val currentWeather = _currentWeather.asStateFlow()

    private val _isLoadingWeather = MutableStateFlow(false)
    val isLoadingWeather = _isLoadingWeather.asStateFlow()

    private var lastWeatherRequestTime = 0L
    private val WEATHER_CACHE_DURATION = 60 * 60 * 1000L
    private var weatherJob: Job? = null

    fun acceptWeatherFromMap(weather: CurrentWeather) {
        if (_currentWeather.value == null) {
            _currentWeather.value = weather
            lastWeatherRequestTime = System.currentTimeMillis()
        }
    }


    private fun calculateDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadiusKm = 6371.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a =
            kotlin.math.sin(dLat / 2).let { it * it } +
                    kotlin.math.cos(Math.toRadians(lat1)) *
                    kotlin.math.cos(Math.toRadians(lat2)) *
                    kotlin.math.sin(dLon / 2).let { it * it }

        return 2 * earthRadiusKm *
                kotlin.math.asin(kotlin.math.sqrt(a.coerceIn(0.0, 1.0)))
    }

    fun updateLocation(lat: Double, lon: Double) {
        val previousLocation = _currentLocation.value
        _currentLocation.value = lat to lon

        val locationChangedSignificantly =
            previousLocation == null ||
                    calculateDistanceKm(
                        previousLocation.first,
                        previousLocation.second,
                        lat,
                        lon
                    ) > 10.0

        val cacheExpired =
            System.currentTimeMillis() - lastWeatherRequestTime >
                    WEATHER_CACHE_DURATION

        if (
            _currentWeather.value == null ||
            locationChangedSignificantly ||
            cacheExpired
        ) {
            loadWeather(lat, lon)
        }
    }

    private fun loadWeather(lat: Double, lon: Double) {
        if (weatherJob?.isActive == true) return

        _isLoadingWeather.value = true

        weatherJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val weather = weatherService.getWeatherData(lat, lon)

                if (
                    weather?.temperature != null &&
                    weather.humidity != null &&
                    weather.avgTemp5Days != null &&
                    weather.avgHumidity5Days != null &&
                    weather.rainSum5Days != null
                ) {
                    _currentWeather.value = CurrentWeather(
                        temperature = weather.temperature,
                        humidity = weather.humidity,
                        avgTemp5Days = weather.avgTemp5Days,
                        avgHumidity5Days = weather.avgHumidity5Days,
                        rainSum5Days = weather.rainSum5Days
                    )

                    // Кешируем только успешный результат
                    lastWeatherRequestTime = System.currentTimeMillis()
                } else {
                    android.util.Log.w(
                        "WEATHER",
                        "Weather response is empty or incomplete"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e(
                    "WEATHER",
                    "Weather request failed",
                    e
                )
            } finally {
                _isLoadingWeather.value = false
            }
        }
    }

}