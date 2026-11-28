
package com.liulkovich.florapoint.shared.domain

data class SharedWeatherData(
    val temperature: Double,
    val humidity: Int,
    val avgTemp5Days: Double,
    val avgHumidity5Days: Int,
    val rainSum5Days: Double
)

fun buildOpenMeteoWeatherUrl(
    latitude: Double,
    longitude: Double
): String {
    return "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$latitude" +
            "&longitude=$longitude" +
            "&current=temperature_2m,relative_humidity_2m" +
            "&daily=temperature_2m_mean,relative_humidity_2m_mean,precipitation_sum" +
            "&past_days=5" +
            "&forecast_days=1" +
            "&timezone=auto"
}
