
package com.liulkovich.florapoint.shared.domain

data class SharedForecastItem(
    val reference: ReferenceModel,
    val score: Int,
    val hasNearbyPoint: Boolean = false,
    val nearbyPointId: Int? = null,
    val nearbyLat: Double? = null,
    val nearbyLon: Double? = null,
    val nearbyDistanceKm: Double? = null
)

enum class SharedSocialForecastError {
    NO_INTERNET,
    SERVICE_UNAVAILABLE
}

data class SharedForecastScreenState(
    val isLoading: Boolean = true,
    val noWeather: Boolean = false,

    val items: List<SharedForecastItem> = emptyList(),

    val temperature: Double = 0.0,
    val humidity: Int = 0,
    val avgTemp: Double = 0.0,
    val avgHumidity: Int = 0,
    val currentMonth: Int = 0,

    val socialLoading: Boolean = false,
    val socialMessage: String? = null,
    val socialSpecies: List<String> = emptyList(),
    val socialError: SharedSocialForecastError? = null
)

