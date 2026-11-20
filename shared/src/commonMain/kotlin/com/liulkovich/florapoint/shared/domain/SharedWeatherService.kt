
package com.liulkovich.florapoint.shared.domain

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.*

private val weatherClient = HttpClient {
    expectSuccess = true
}

suspend fun loadSharedWeather(
    latitude: Double,
    longitude: Double
): SharedWeatherData? {

    return try {
        val url = buildOpenMeteoWeatherUrl(
            latitude = latitude,
            longitude = longitude
        )

        val response = weatherClient.get(url)
        val json = Json.parseToJsonElement(
            response.bodyAsText()
        ).jsonObject

        val current = json["current"]?.jsonObject
            ?: return null

        val daily = json["daily"]?.jsonObject
            ?: return null

        val temperature = current["temperature_2m"]
            ?.jsonPrimitive?.doubleOrNull
            ?: return null

        val humidity = current["relative_humidity_2m"]
            ?.jsonPrimitive?.intOrNull
            ?: return null

        fun dailyNumbers(key: String): List<Double>? {
            val array = daily[key]?.jsonArray ?: return null

            return array.map { element ->
                element.jsonPrimitive.doubleOrNull
                    ?: return null
            }
        }

        val temperatures = dailyNumbers(
            "temperature_2m_mean"
        ) ?: return null

        val humidities = dailyNumbers(
            "relative_humidity_2m_mean"
        ) ?: return null

        val precipitation = daily["precipitation_sum"]
            ?.jsonArray
            ?.map { it.jsonPrimitive.doubleOrNull ?: 0.0 }
            ?: return null

        if (
            temperatures.size < 6 ||
            humidities.size < 6 ||
            precipitation.size < 6
        ) {
            return null
        }

        SharedWeatherData(
            temperature = temperature,
            humidity = humidity,
            avgTemp5Days = temperatures
                .take(5)
                .average(),
            avgHumidity5Days = humidities
                .take(5)
                .average()
                .toInt(),
            rainSum5Days = precipitation
                .take(5)
                .sum()
        )

    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
