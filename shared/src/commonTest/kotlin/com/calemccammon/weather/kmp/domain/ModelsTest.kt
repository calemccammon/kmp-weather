package com.calemccammon.weather.kmp.domain

import com.calemccammon.weather.kmp.domain.models.Place
import com.calemccammon.weather.kmp.domain.models.Units
import com.calemccammon.weather.kmp.testing.sampleForecast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModelsTest {
    @Test
    fun `a place picked off the map has coordinates and no name`() {
        val place = Place.fromCoordinates(35.6, -82.55)

        assertEquals(Place(latitude = 35.6, longitude = -82.55), place)
        assertNull(place.name)
    }

    @Test
    fun `a longitude from a map dragged round the world is brought back into range`() {
        assertEquals(-170.0, Place.fromCoordinates(0.0, 190.0).longitude)
        assertEquals(170.0, Place.fromCoordinates(0.0, -190.0).longitude)
        assertEquals(10.0, Place.fromCoordinates(0.0, 730.0).longitude)
        assertEquals(-180.0, Place.normalizeLongitude(180.0))
        assertEquals(-180.0, Place.normalizeLongitude(-180.0))
        assertEquals(-82.55, Place.normalizeLongitude(-82.55))
    }

    @Test
    fun `today is the day the current conditions were measured on`() {
        val forecast = sampleForecast()

        assertTrue(forecast.isToday(forecast.days[0]))
        assertFalse(forecast.isToday(forecast.days[1]))
    }

    @Test
    fun `only the countries that use Fahrenheit start in imperial units`() {
        assertEquals(Units.Imperial, Units.forCountry("US"))
        assertEquals(Units.Imperial, Units.forCountry("LR"))
        assertEquals(Units.Imperial, Units.forCountry("MM"))
        assertEquals(Units.Metric, Units.forCountry("GB"))
        assertEquals(Units.Metric, Units.forCountry(null))
    }
}
