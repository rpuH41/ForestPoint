
package com.liulkovich.florapoint

import com.liulkovich.florapoint.domain.UserPoints
import com.liulkovich.florapoint.domain.toReferenceModel
import com.liulkovich.florapoint.presentation.screens.map.MapScreenState

import com.liulkovich.florapoint.shared.domain.SharedMapPoint
import com.liulkovich.florapoint.shared.domain.SharedMapPointsState
import com.liulkovich.florapoint.shared.domain.localizedName

fun MapScreenState.toSharedMapPointsState(
    language: String,
    customSpeciesName: String,
    unknownSpeciesName: String,
    isPublicForeignPoint: (UserPoints) -> Boolean
): SharedMapPointsState {

    val speciesById = species.associateBy { it.id }

    val sharedPoints = sortedPointsWithDistance.map { (point, distance) ->

        val reference = point.speciesId?.let { speciesId ->
            speciesById[speciesId]
        }

        val localizedSpeciesName = reference
            ?.toReferenceModel()
            ?.localizedName(language)

        val displayName = when {
            point.speciesId == 0 && point.userName.isNotBlank() ->
                point.userName

            point.speciesId == 0 ->
                customSpeciesName

            !localizedSpeciesName.isNullOrBlank() ->
                localizedSpeciesName

            point.userName.isNotBlank() ->
                point.userName

            else ->
                unknownSpeciesName
        }

        val pointKey = if (point.id != 0) {
            "local_${point.id}"
        } else {
            "public_${point.cloudId ?: point.hashCode()}"
        }

        SharedMapPoint(
            key = pointKey,
            id = point.id,
            speciesId = point.speciesId,

            displayName = displayName,
            userName = point.userName,
            description = point.description,
            category = point.category ?: "custom",

            latitude = point.latitude,
            longitude = point.longitude,
            timestampSeconds = point.timestamp,

            distanceKm = distance,
            isSelected = point.id == selectedPointId,

            isOwner = !isPublicForeignPoint(point),
            isPublic = point.isPublic,

            temperature = point.temperature,
            humidity = point.humidity,
            avgTemp5Days = point.avgTemp5Days,
            avgHumidity5Days = point.avgHumidity5Days
        )
    }

    return SharedMapPointsState(
        points = sharedPoints,
        searchQuery = searchQuery,
        selectedPointKey = sharedPoints
            .firstOrNull { it.isSelected }
            ?.key
    )
}
