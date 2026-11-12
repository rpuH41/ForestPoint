
package com.liulkovich.florapoint.shared.domain

data class SharedOfflineRegion(
    val id: String,
    val name: String,
    val tilesCount: Int,
    val sizeLabel: String,
    val createdAtLabel: String
)

data class SharedOfflineRegionsState(
    val regions: List<SharedOfflineRegion> = emptyList()
)
