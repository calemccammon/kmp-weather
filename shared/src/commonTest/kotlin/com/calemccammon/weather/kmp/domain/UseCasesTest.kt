package com.calemccammon.weather.kmp.domain

import com.calemccammon.weather.kmp.domain.models.Place
import com.calemccammon.weather.kmp.domain.models.Units
import com.calemccammon.weather.kmp.domain.repositories.WeatherException
import com.calemccammon.weather.kmp.domain.repositories.WeatherFailure
import com.calemccammon.weather.kmp.domain.usecases.GetForecast
import com.calemccammon.weather.kmp.domain.usecases.GetUnits
import com.calemccammon.weather.kmp.domain.usecases.SearchPlaces
import com.calemccammon.weather.kmp.domain.usecases.SetUnits
import com.calemccammon.weather.kmp.testing.FakeSettingsRepository
import com.calemccammon.weather.kmp.testing.FakeWeatherRepository
import com.calemccammon.weather.kmp.testing.ForecastRequest
import com.calemccammon.weather.kmp.testing.asheville
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UseCasesTest {
    private val weather = FakeWeatherRepository()
    private val settings = FakeSettingsRepository()

    @Test
    fun `a search asks for the query without the spaces around it`() =
        runTest {
            weather.placesToReturn = listOf(asheville)

            assertEquals(listOf(asheville), SearchPlaces(weather)("  ashev "))
            assertEquals(listOf("ashev"), weather.searchQueries)
        }

    @Test
    fun `a query of one letter finds nothing and asks nobody`() =
        runTest {
            assertEquals(emptyList(), SearchPlaces(weather)(" a "))
            assertEquals(emptyList(), weather.searchQueries)
        }

    @Test
    fun `a forecast is asked for at the place in the units given`() =
        runTest {
            GetForecast(weather)(asheville, Units.Imperial)

            assertEquals(listOf(ForecastRequest(35.6, -82.55, Units.Imperial)), weather.forecastRequests)
        }

    @Test
    fun `a forecast is asked for with the longitude in range`() =
        runTest {
            GetForecast(weather)(Place(latitude = 10.0, longitude = 190.0), Units.Metric)

            assertEquals(-170.0, weather.forecastRequests.single().longitude)
        }

    @Test
    fun `a point off the globe is refused without asking the repository`() =
        runTest {
            for (place in listOf(
                Place(latitude = 90.5, longitude = 0.0),
                Place(latitude = Double.NaN, longitude = 0.0),
                Place(latitude = 0.0, longitude = Double.POSITIVE_INFINITY),
                Place(latitude = 0.0, longitude = Double.NaN),
            )) {
                val error = assertFailsWith<WeatherException> { GetForecast(weather)(place, Units.Metric) }
                assertEquals(WeatherFailure.InvalidPlace, error.failure)
            }
            assertEquals(emptyList(), weather.forecastRequests)
        }

    @Test
    fun `units follow the device's country until the user chooses`() {
        assertEquals(Units.Imperial, GetUnits(settings)(deviceCountry = "US"))
        assertEquals(Units.Metric, GetUnits(settings)(deviceCountry = "FR"))

        SetUnits(settings)(Units.Metric)

        assertEquals(Units.Metric, GetUnits(settings)(deviceCountry = "US"))
    }
}
