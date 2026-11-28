
package com.liulkovich.florapoint

import com.liulkovich.florapoint.presentation.screens.map.GoogleMapLayer
import com.liulkovich.florapoint.presentation.screens.map.MapProvider
import com.liulkovich.florapoint.presentation.screens.map.MapScreenState

import com.liulkovich.florapoint.shared.domain.SharedGoogleMapLayer
import com.liulkovich.florapoint.shared.domain.SharedMapProvider
import com.liulkovich.florapoint.shared.domain.SharedMapSettings

// Android MapScreenState -> KMP SharedMapSettings

fun MapScreenState.toSharedMapSettings(): SharedMapSettings {
    return SharedMapSettings(
        provider = when (mapProvider) {
            MapProvider.OSM ->
                SharedMapProvider.OSM

            MapProvider.GOOGLE ->
                SharedMapProvider.GOOGLE
        },

        googleLayer = when (googleMapLayer) {
            GoogleMapLayer.NORMAL ->
                SharedGoogleMapLayer.NORMAL

            GoogleMapLayer.SATELLITE ->
                SharedGoogleMapLayer.SATELLITE

            GoogleMapLayer.HYBRID ->
                SharedGoogleMapLayer.HYBRID

            GoogleMapLayer.TERRAIN ->
                SharedGoogleMapLayer.TERRAIN
        }
    )
}

// KMP SharedMapProvider -> Android MapProvider

fun SharedMapProvider.toAndroidMapProvider(): MapProvider {
    return when (this) {
        SharedMapProvider.OSM ->
            MapProvider.OSM

        SharedMapProvider.GOOGLE ->
            MapProvider.GOOGLE
    }
}

// KMP SharedGoogleMapLayer -> Android GoogleMapLayer

fun SharedGoogleMapLayer.toAndroidGoogleMapLayer(): GoogleMapLayer {
    return when (this) {
        SharedGoogleMapLayer.NORMAL ->
            GoogleMapLayer.NORMAL

        SharedGoogleMapLayer.SATELLITE ->
            GoogleMapLayer.SATELLITE

        SharedGoogleMapLayer.HYBRID ->
            GoogleMapLayer.HYBRID

        SharedGoogleMapLayer.TERRAIN ->
            GoogleMapLayer.TERRAIN
    }
}
