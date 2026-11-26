package com.calemccammon.weather.kmp.domain

import com.calemccammon.weather.kmp.domain.models.DailyForecast
import com.calemccammon.weather.kmp.domain.models.Place
import com.calemccammon.weather.kmp.domain.models.Units
import com.calemccammon.weather.kmp.domain.models.WeatherCondition
import com.calemccammon.weather.kmp.testing.sampleForecast
import kotlinx.datetime.LocalDate
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
    fun `a metric forecast converts to Fahrenheit and mph on the device`() {
        val snowy = DailyForecast(LocalDate(2026, 11, 18), low = -40.0, high = 0.0, condition = WeatherCondition.Snow)
        val sample = sampleForecast(temperature = 10.0, days = listOf(snowy))
        val forecast = sample.copy(current = sample.current.copy(feelsLike = 20.0, windSpeed = 20.0))

        val imperial = forecast.inUnits(Units.Imperial)

        assertEquals(50.0, imperial.current.temperature, absoluteTolerance = 1e-9)
        assertEquals(68.0, imperial.current.feelsLike, absoluteTolerance = 1e-9)
        assertEquals(12.43, imperial.current.windSpeed, absoluteTolerance = 0.01)
        assertEquals(-40.0, imperial.days.single().low, absoluteTolerance = 1e-9)
        assertEquals(32.0, imperial.days.single().high, absoluteTolerance = 1e-9)
        assertEquals(forecast.current.observedAt, imperial.current.observedAt)
        assertEquals(forecast.days.single().date, imperial.days.single().date)
    }

    @Test
    fun `a precipitation chance is a percentage in any units`() {
        val forecast = sampleForecast()

        assertEquals(
            forecast.days.map { it.precipitationChance },
            forecast.inUnits(Units.Imperial).days.map { it.precipitationChance },
        )
    }

    @Test
    fun `a metric forecast in metric units is unchanged`() {
        val forecast = sampleForecast()

        assertEquals(forecast, forecast.inUnits(Units.Metric))
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
