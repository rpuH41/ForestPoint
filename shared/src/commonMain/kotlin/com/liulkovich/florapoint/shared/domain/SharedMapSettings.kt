
package com.liulkovich.florapoint.shared.domain

enum class SharedMapProvider {
    OSM,
    GOOGLE
}

enum class SharedGoogleMapLayer {
    NORMAL,
    SATELLITE,
    HYBRID,
    TERRAIN
}

data class SharedMapSettings(
    val provider: SharedMapProvider = SharedMapProvider.OSM,
    val googleLayer: SharedGoogleMapLayer = SharedGoogleMapLayer.NORMAL
)
