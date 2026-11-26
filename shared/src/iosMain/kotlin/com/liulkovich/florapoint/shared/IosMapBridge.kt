
package com.liulkovich.florapoint.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import platform.UIKit.UIView
import com.liulkovich.florapoint.shared.domain.SharedMapPoint



interface IosMapTapListener {
    fun onMapTap(latitude: Double, longitude: Double)
}
interface IosMapBridge {
    fun makeView(): UIView

    fun centerOn(latitude: Double, longitude: Double)

    fun zoomIn()

    fun zoomOut()

    fun updatePoints(points: List<SharedMapPoint>)

    fun setMapTapListener(listener: IosMapTapListener?)

    fun sharePoint(
        latitude: Double,
        longitude: Double,
        title: String
    )
}

interface IosGoogleMapBridge : IosMapBridge {
    fun setGoogleLayer(layerKey: String)
}

@Composable
fun IosOsmMapContent(
    mapBridge: IosMapBridge,
    modifier: Modifier = Modifier
) {
    UIKitView(
        factory = { mapBridge.makeView() },
        modifier = modifier
    )
}
