
package com.liulkovich.florapoint.shared.domain

data class SharedMapPoint(
    val key: String,
    val id: Int,
    val speciesId: Int?,

    val displayName: String,
    val userName: String,
    val description: String,
    val category: String,

    val latitude: Double,
    val longitude: Double,
    val timestampSeconds: Long,

    val distanceKm: Double? = null,
    val isSelected: Boolean = false,
    val isOwner: Boolean = true,
    val isPublic: Boolean = false,

    val temperature: Double? = null,
    val humidity: Int? = null,
    val avgTemp5Days: Double? = null,
    val avgHumidity5Days: Int? = null
)

data class SharedMapPointsState(
    val points: List<SharedMapPoint> = emptyList(),
    val searchQuery: String = "",
    val selectedPointKey: String? = null
)
