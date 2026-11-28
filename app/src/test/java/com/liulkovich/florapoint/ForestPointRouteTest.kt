
package com.liulkovich.florapoint

import com.liulkovich.florapoint.shared.ui.navigation.ForestPointRoute
import org.junit.Assert.assertEquals
import org.junit.Test

class ForestPointRouteTest {

    @Test
    fun homeRouteMatchesAndroid() {
        assertEquals("Home", ForestPointRoute.Home.route)
    }

    @Test
    fun guideRouteWithoutCategory() {
        assertEquals(
            "Guide",
            ForestPointRoute.Guide.createRoute()
        )
    }

    @Test
    fun guideRouteWithCategory() {
        assertEquals(
            "Guide?category=mushroom",
            ForestPointRoute.Guide.createRoute("mushroom")
        )
    }

    @Test
    fun detailRouteUsesSpeciesId() {
        assertEquals(
            "Detail/37",
            ForestPointRoute.Detail.createRoute(37)
        )
    }

    @Test
    fun mainTabRoutesStayUnchanged() {
        assertEquals("Map", ForestPointRoute.Map.route)
        assertEquals("Forecast", ForestPointRoute.Forecast.route)
        assertEquals("Settings", ForestPointRoute.Settings.route)
    }
}
