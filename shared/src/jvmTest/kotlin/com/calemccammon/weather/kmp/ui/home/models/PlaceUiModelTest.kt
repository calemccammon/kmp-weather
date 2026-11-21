package com.calemccammon.weather.kmp.ui.home.models

import com.calemccammon.weather.kmp.domain.models.Place
import com.calemccammon.weather.kmp.testing.asheville
import com.calemccammon.weather.kmp.testing.words
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaceUiModelTest {
    private fun titleOf(
        latitude: Double,
        longitude: Double,
    ) = PlaceUiModel.from(Place.fromCoordinates(latitude, longitude)).title.words

    @Test
    fun `a named place shows its name with its region and country under it`() {
        val place = PlaceUiModel.from(asheville)

        assertEquals("Asheville", place.title.words)
        assertEquals("North Carolina, United States", place.subtitle)
        assertEquals(35.6 to -82.55, place.latitude to place.longitude)
    }

    @Test
    fun `whatever is missing is left out of the line under the name`() {
        assertEquals(
            "Australia",
            PlaceUiModel.from(Place(-33.87, 151.21, name = "Sydney", country = "Australia")).subtitle,
        )
        assertEquals("", PlaceUiModel.from(Place(0.0, 0.0, name = "Null Island")).subtitle)
    }

    @Test
    fun `a place with no name is called by its coordinates`() {
        assertEquals("35.60°N, 82.55°W", titleOf(35.6, -82.55))
        assertEquals("33.87°S, 151.21°E", titleOf(-33.868, 151.209))
        assertEquals("0.05°N, 9.00°E", titleOf(0.05, 9.0))
        assertEquals("", PlaceUiModel.from(Place.fromCoordinates(35.6, -82.55)).subtitle)
    }

    @Test
    fun `a point a hair off the equator or Greenwich is on the same side as the line itself`() {
        assertEquals("0.00°N, 0.00°E", titleOf(0.0, 0.0))
        assertEquals("0.00°N, 0.00°E", titleOf(-0.001, -0.004))
        assertEquals("0.01°S, 0.01°W", titleOf(-0.006, -0.005001))
    }
}
