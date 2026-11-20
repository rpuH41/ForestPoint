
package com.liulkovich.florapoint.shared.domain

fun calculateSharedForecast(
    species: List<ReferenceModel>,
    conditions: List<SharedSpeciesConditions>,
    weather: SharedWeatherData,
    currentMonth: Int
): List<SharedForecastItem> {

    val conditionsById = conditions.associateBy {
        it.speciesId
    }

    return species.mapNotNull { item ->

        val condition = conditionsById[item.id]
            ?: return@mapNotNull null

        val inSeason = if (item.startMonth <= item.endMonth) {
            currentMonth in item.startMonth..item.endMonth
        } else {
            currentMonth >= item.startMonth ||
                    currentMonth <= item.endMonth
        }

        if (!inSeason) return@mapNotNull null

        val tempScore = when {
            weather.avgTemp5Days < condition.tempMin - 4 ||
                    weather.avgTemp5Days > condition.tempMax + 4 -> 0

            weather.avgTemp5Days >= condition.tempMin &&
                    weather.avgTemp5Days <= condition.tempMax -> 35

            else -> 15
        }

        val humidityScore = when {
            weather.avgHumidity5Days < condition.humidityMin - 12 ||
                    weather.avgHumidity5Days > condition.humidityMax + 12 -> 0

            weather.avgHumidity5Days in
                    condition.humidityMin..condition.humidityMax -> 25

            else -> 12
        }

        var score = 45 + tempScore + humidityScore

        if (tempScore == 0 || humidityScore == 0) {
            score = score.coerceAtMost(79)
        }

        if (weather.rainSum5Days <= 0.0) {
            score = score.coerceAtMost(79)
        }

        score = score.coerceIn(0, 100)

        if (score < 25) return@mapNotNull null

        SharedForecastItem(
            reference = item,
            score = score
        )
    }.sortedByDescending { it.score }
}
