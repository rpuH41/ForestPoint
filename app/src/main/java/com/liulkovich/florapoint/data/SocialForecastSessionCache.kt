package com.liulkovich.florapoint.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialForecastSessionCache @Inject constructor() {

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

        /*
         * Небольшое изменение GPS не должно
         * запускать тяжёлый запрос заново.
         */
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
        val earthRadiusKm = 6371.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a =
            sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) *
                    cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) *
                    sin(dLon / 2)

        return earthRadiusKm * 2 *
                atan2(
                    sqrt(a),
                    sqrt(1 - a)
                )
    }
}