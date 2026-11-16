
package com.liulkovich.florapoint

import com.liulkovich.florapoint.domain.toReferenceModel
import com.liulkovich.florapoint.presentation.screens.forecast.ForecastScreenState
import com.liulkovich.florapoint.presentation.screens.forecast.SocialForecastError
import com.liulkovich.florapoint.shared.domain.SharedForecastItem
import com.liulkovich.florapoint.shared.domain.SharedForecastScreenState
import com.liulkovich.florapoint.shared.domain.SharedSocialForecastError

fun ForecastScreenState.toSharedForecastScreenState():
        SharedForecastScreenState {

    return SharedForecastScreenState(
        isLoading = isLoading,
        noWeather = noWeather,

        items = items.map { forecastItem ->
            SharedForecastItem(
                reference = forecastItem.reference.toReferenceModel(),
                score = forecastItem.score,
                hasNearbyPoint = forecastItem.hasNearbyPoint,
                nearbyPointId = forecastItem.nearbyPointId,
                nearbyLat = forecastItem.nearbyLat,
                nearbyLon = forecastItem.nearbyLon,
                nearbyDistanceKm = forecastItem.nearbyDistanceKm
            )
        },

        temperature = temperature,
        humidity = humidity,
        avgTemp = avgTemp,
        avgHumidity = avgHumidity,
        currentMonth = currentMonth,

        socialLoading = socialLoading,
        socialMessage = socialMessage,
        socialSpecies = socialSpecies,

        socialError = when (socialError) {
            SocialForecastError.NO_INTERNET ->
                SharedSocialForecastError.NO_INTERNET

            SocialForecastError.SERVICE_UNAVAILABLE ->
                SharedSocialForecastError.SERVICE_UNAVAILABLE

            null -> null
        }
    )
}
