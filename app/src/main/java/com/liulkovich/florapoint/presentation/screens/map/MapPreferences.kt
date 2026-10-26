
package com.liulkovich.florapoint.presentation.screens.map

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.mapPreferencesDataStore by preferencesDataStore(
    name = "map_preferences"
)

data class SavedMapSettings(
    val provider: MapProvider = MapProvider.OSM,
    val layer: GoogleMapLayer = GoogleMapLayer.NORMAL
)

class MapPreferences(private val context: Context) {

    private val providerKey = stringPreferencesKey("map_provider")
    private val layerKey = stringPreferencesKey("google_map_layer")

    val settings: Flow<SavedMapSettings> =
        context.mapPreferencesDataStore.data.map { preferences ->

            val provider = runCatching {
                MapProvider.valueOf(
                    preferences[providerKey] ?: MapProvider.OSM.name
                )
            }.getOrDefault(MapProvider.OSM)

            val layer = runCatching {
                GoogleMapLayer.valueOf(
                    preferences[layerKey] ?: GoogleMapLayer.NORMAL.name
                )
            }.getOrDefault(GoogleMapLayer.NORMAL)

            SavedMapSettings(
                provider = provider,
                layer = layer
            )
        }

    suspend fun saveProvider(provider: MapProvider) {
        context.mapPreferencesDataStore.edit { preferences ->
            preferences[providerKey] = provider.name
        }
    }

    suspend fun saveLayer(layer: GoogleMapLayer) {
        context.mapPreferencesDataStore.edit { preferences ->
            preferences[layerKey] = layer.name
        }
    }
}
