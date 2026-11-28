
package com.liulkovich.florapoint.shared.domain

data class SharedSpeciesConditions(
    val speciesId: Int,
    val tempMin: Int,
    val tempMax: Int,
    val humidityMin: Int,
    val humidityMax: Int
)
