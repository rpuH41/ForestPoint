package com.liulkovich.florapoint.shared

import com.liulkovich.florapoint.shared.domain.SharedOfflineRegion

/** Native iOS access to locally stored raster MBTiles map packs. */
interface IosOfflineMapsBridge {
    fun isNetworkUnavailable(): Boolean
    fun listRegions(): List<SharedOfflineRegion>
    fun deleteRegion(id: String): Boolean
    fun pickRegion()
    fun selectAndDownloadRegion()
    fun openDownloadedMap()
}
