
package com.liulkovich.florapoint.shared.domain

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class SharedSocialForecastSessionCache {

    data class CachedForecast(
        val latitude: Double,
        val longitude: Double,
        val language: String,
        val message: String,
        val species: List<String>
    )

    private var cachedForecast: CachedForecast? = null

    fun get(
        latitude: Double,
        longitude: Double,
        language: String
    ): CachedForecast? {
        val cached = cachedForecast ?: return null

        if (cached.language != language) {
            return null
        }

        val distance = distanceKm(
            latitude,
            longitude,
            cached.latitude,
            cached.longitude
        )

        if (distance > 2.0) {
            return null
        }

        return cached
    }

    fun save(
        latitude: Double,
        longitude: Double,
        language: String,
        message: String,
        species: List<String>
    ) {
        cachedForecast = CachedForecast(
            latitude = latitude,
            longitude = longitude,
            language = language,
            message = message,
            species = species
        )
    }

    fun clear() {
        cachedForecast = null
    }

    private fun distanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val radiusKm = 6371.0

        fun radians(value: Double): Double =
            value * PI / 180.0

        val dLat = radians(lat2 - lat1)
        val dLon = radians(lon2 - lon1)

        val a =
            sin(dLat / 2) * sin(dLat / 2) +
                    cos(radians(lat1)) *
                    cos(radians(lat2)) *
                    sin(dLon / 2) *
                    sin(dLon / 2)

        return radiusKm * 2.0 *
                atan2(
                    sqrt(a.coerceIn(0.0, 1.0)),
                    sqrt((1.0 - a).coerceAtLeast(0.0))
                )
    }
}
