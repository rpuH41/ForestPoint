
package com.liulkovich.florapoint.shared.ui.navigation

sealed class ForestPointRoute(val route: String) {

    data object Home : ForestPointRoute("Home")

    data object Guide : ForestPointRoute("Guide?category={category}") {
        fun createRoute(category: String = ""): String =
            if (category.isEmpty()) "Guide"
            else "Guide?category=$category"
    }

    data object Map : ForestPointRoute("Map")

    data object MapDeepLink : ForestPointRoute(
        "Map?lat={lat}&lon={lon}&name={name}&category={category}"
    )

    data object Forecast : ForestPointRoute("Forecast")

    data object Settings : ForestPointRoute("Settings")

    data object Notifications : ForestPointRoute("Notifications")

    data object Detail : ForestPointRoute("Detail/{speciesId}") {
        fun createRoute(speciesId: Int): String =
            "Detail/$speciesId"
    }

    data object OfflineRegions : ForestPointRoute("OfflineRegions")

    data object DownloadArea : ForestPointRoute("DownloadArea")
}
