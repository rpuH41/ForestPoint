
package com.liulkovich.florapoint.shared.domain

data class SharedUserPoint(
    val id: Int = 0,
    val speciesId: Int? = null,

    val latitude: Double,
    val longitude: Double,
    val userName: String,
    val description: String,
    val timestamp: Long,

    val isFavorite: Int = 0,
    val photoPath: String = "",
    val accuracy: Int = 0,
    val category: String? = "custom",

    val temperature: Double? = null,
    val humidity: Int? = null,
    val avgTemp5Days: Double? = null,
    val avgHumidity5Days: Int? = null,
    val weatherTimestamp: Long? = null,

    val isPublic: Boolean = false,
    val ownerUid: String? = null,
    val cloudId: String? = null,
    val syncState: String = "LOCAL"
)
