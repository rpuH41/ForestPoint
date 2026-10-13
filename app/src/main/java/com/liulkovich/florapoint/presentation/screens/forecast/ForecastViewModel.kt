package com.liulkovich.florapoint.presentation.screens.forecast

import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liulkovich.florapoint.data.remote.SocialForecastRepository
import com.liulkovich.florapoint.data.SocialForecastSessionCache
import com.liulkovich.florapoint.domain.ForecastItem
import com.liulkovich.florapoint.domain.FloraRepository
import com.liulkovich.florapoint.domain.GetForecastUseCase
import com.liulkovich.florapoint.presentation.screens.map.CurrentWeather
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import android.util.Log
import com.liulkovich.florapoint.data.remote.NoInternetException
import com.liulkovich.florapoint.data.remote.SocialForecastServiceException

@HiltViewModel
class ForecastViewModel @Inject constructor(
    private val getForecastUseCase: GetForecastUseCase,
    private val repository: FloraRepository,
    private val socialForecastRepository: SocialForecastRepository,
    private val socialForecastCache: SocialForecastSessionCache
) : ViewModel() {

    private val _state = MutableStateFlow(ForecastScreenState())
    val state = _state.asStateFlow()

    /** Язык интерфейса: "ru" или "en". */
    private val appLanguage: String
        get() {
            val lang = LocaleListCompat.getAdjustedDefault()
                .get(0)
                ?.language
                ?: "ru"
            return if (lang == "en") "en" else "ru"
        }

    fun load(
        weather: CurrentWeather?,
        isWeatherLoading: Boolean,
        currentLat: Double,
        currentLon: Double
    ) {
        if (weather == null && isWeatherLoading) {
            _state.update { it.copy(isLoading = true, noWeather = false) }
            return
        }

        if (weather == null) {
            _state.update { it.copy(isLoading = false, noWeather = true) }
            return
        }

        viewModelScope.launch {

            val language = appLanguage

            val cachedSocialForecast =
                socialForecastCache.get(
                    latitude = currentLat,
                    longitude = currentLon,
                    language = language
                )

            _state.update {

                it.copy(
                    isLoading = true,

                    socialLoading =
                        cachedSocialForecast == null,

                    socialMessage =
                        cachedSocialForecast?.message
                            ?: it.socialMessage,

                    socialSpecies =
                        cachedSocialForecast?.species
                            ?: it.socialSpecies,

                    socialError = null
                )
            }

            val currentMonth =
                Calendar.getInstance()
                    .get(Calendar.MONTH) + 1

            val userPoints =
                repository.getAllUserPointsList()

            val weatherForecastDeferred = async {
                getForecastUseCase(
                    temperature = weather.temperature,
                    humidity = weather.humidity,
                    avgTemp = weather.avgTemp5Days,
                    avgHumidity = weather.avgHumidity5Days,
                    rainSum5Days = weather.rainSum5Days,
                    currentMonth = currentMonth,
                    currentLat = currentLat,
                    currentLon = currentLon,
                    userPoints = userPoints
                )
            }

            val socialForecastDeferred =
                if (cachedSocialForecast == null) {
                    async {
                        socialForecastRepository.getSocialForecast(
                            lat = currentLat,
                            lon = currentLon,
                            language = language
                        )
                    }
                } else {
                    null
                }

            val forecast = weatherForecastDeferred.await()

            _state.update {
                it.copy(
                    isLoading = false,
                    noWeather = false,
                    items = forecast,
                    temperature = weather.temperature,
                    humidity = weather.humidity,
                    avgTemp = weather.avgTemp5Days,
                    avgHumidity = weather.avgHumidity5Days,
                    currentMonth = currentMonth
                )
            }

            if (socialForecastDeferred != null) {

                val socialResult =
                    socialForecastDeferred.await()

                socialResult
                    .onSuccess { social ->

                        socialForecastCache.save(
                            latitude = currentLat,
                            longitude = currentLon,
                            language = language,
                            message = social.message,
                            species = social.species
                        )

                        _state.update {
                            it.copy(
                                socialLoading = false,
                                socialMessage = social.message,
                                socialSpecies = social.species,
                                socialError = null
                            )
                        }
                    }
                    .onFailure { error ->

                        Log.e(
                            "SocialForecast",
                            "Firebase forecast failed",
                            error
                        )

                        val socialError =
                            when (error) {
                                is NoInternetException ->
                                    SocialForecastError.NO_INTERNET

                                is SocialForecastServiceException ->
                                    SocialForecastError.SERVICE_UNAVAILABLE

                                else ->
                                    SocialForecastError.SERVICE_UNAVAILABLE
                            }

                        _state.update {
                            it.copy(
                                socialLoading = false,
                                socialMessage = null,
                                socialSpecies = emptyList(),
                                socialError = socialError
                            )
                        }
                    }
            }
        }
    }
}

enum class SocialForecastError {
    NO_INTERNET,
    SERVICE_UNAVAILABLE
}

data class ForecastScreenState(
    val isLoading: Boolean = true,
    val noWeather: Boolean = false,
    val items: List<ForecastItem> = emptyList(),
    val temperature: Double = 0.0,
    val humidity: Int = 0,
    val avgTemp: Double = 0.0,
    val avgHumidity: Int = 0,
    val currentMonth: Int = 0,
    val socialLoading: Boolean = false,
    val socialMessage: String? = null,
    val socialSpecies: List<String> = emptyList(),
    val socialError: SocialForecastError? = null
)