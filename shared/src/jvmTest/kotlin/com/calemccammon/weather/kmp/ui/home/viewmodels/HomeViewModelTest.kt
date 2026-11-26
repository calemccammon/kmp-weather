package com.calemccammon.weather.kmp.ui.home.viewmodels

import com.calemccammon.weather.kmp.domain.models.Units
import com.calemccammon.weather.kmp.domain.repositories.WeatherException
import com.calemccammon.weather.kmp.domain.repositories.WeatherFailure
import com.calemccammon.weather.kmp.domain.usecases.GetForecast
import com.calemccammon.weather.kmp.domain.usecases.GetUnits
import com.calemccammon.weather.kmp.domain.usecases.SetUnits
import com.calemccammon.weather.kmp.testing.FakeSettingsRepository
import com.calemccammon.weather.kmp.testing.FakeWeatherRepository
import com.calemccammon.weather.kmp.testing.ForecastRequest
import com.calemccammon.weather.kmp.testing.MainDispatcherTest
import com.calemccammon.weather.kmp.testing.asheville
import com.calemccammon.weather.kmp.testing.sampleForecast
import com.calemccammon.weather.kmp.testing.sydney
import com.calemccammon.weather.kmp.testing.words
import com.calemccammon.weather.kmp.ui.home.models.ForecastState
import com.calemccammon.weather.kmp.ui.home.models.HomeState
import com.calemccammon.weather.kmp.ui.home.models.UnitsUiModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class HomeViewModelTest : MainDispatcherTest() {
    private val weather = FakeWeatherRepository()
    private val settings = FakeSettingsRepository()

    private fun viewModel(deviceCountry: String? = "US") =
        HomeViewModel(GetForecast(weather), GetUnits(settings), SetUnits(settings), deviceCountry)

    private val HomeViewModel.shown: HomeState
        get() = state.value

    /** The temperature on screen, or null when no forecast is. */
    private val HomeViewModel.temperature: String?
        get() =
            (shown.forecast as? ForecastState.Loaded)
                ?.forecast
                ?.current
                ?.temperature
                ?.words

    @Test
    fun `it starts with no place in the units of the device's country`() {
        assertEquals(HomeState(units = UnitsUiModel.Fahrenheit), viewModel(deviceCountry = "US").shown)
        assertEquals(HomeState(units = UnitsUiModel.Celsius), viewModel(deviceCountry = "DE").shown)
    }

    @Test
    fun `it starts in the units the user chose before whatever the device's country`() {
        settings.units = Units.Metric

        assertEquals(UnitsUiModel.Celsius, viewModel(deviceCountry = "US").shown.units)
    }

    @Test
    fun `selecting a place shows it at once and its forecast when that arrives`() =
        runTest {
            val viewModel = viewModel()

            viewModel.select(asheville)

            assertEquals(
                "Asheville",
                viewModel.shown.place
                    ?.title
                    ?.words,
            )
            assertEquals(ForecastState.Loading, viewModel.shown.forecast)

            advanceUntilIdle()

            assertEquals("36°F", viewModel.temperature)
            assertEquals(listOf(ForecastRequest(35.6, -82.55)), weather.forecastRequests)
        }

    @Test
    fun `a point picked off the map is selected by its coordinates`() =
        runTest {
            val viewModel = viewModel()

            viewModel.selectPoint(35.6, 277.45)
            advanceUntilIdle()

            assertEquals(
                "35.60°N, 82.55°W",
                viewModel.shown.place
                    ?.title
                    ?.words,
            )
            assertEquals(35.6, weather.forecastRequests.single().latitude)
            assertEquals(-82.55, weather.forecastRequests.single().longitude, absoluteTolerance = 1e-9)
        }

    @Test
    fun `a failure is worded for the user and another try can succeed`() =
        runTest {
            val viewModel = viewModel()
            weather.failure = WeatherException(WeatherFailure.Timeout)

            viewModel.select(asheville)
            advanceUntilIdle()

            val failed = assertIs<ForecastState.Failed>(viewModel.shown.forecast)
            assertEquals("The weather service took too long to answer.", failed.message.words)
            assertEquals(
                "Asheville",
                viewModel.shown.place
                    ?.title
                    ?.words,
            )

            weather.failure = null
            viewModel.retryForecast()

            assertEquals(ForecastState.Loading, viewModel.shown.forecast)
            advanceUntilIdle()
            assertEquals("36°F", viewModel.temperature)
        }

    @Test
    fun `the answer for a place left behind never replaces the newer place's`() =
        runTest {
            val viewModel = viewModel()
            weather.forecastToReturn = null

            viewModel.select(asheville)
            advanceUntilIdle()
            viewModel.select(sydney)
            advanceUntilIdle()
            weather.forecasts[0].complete(sampleForecast(temperature = 1.0))
            advanceUntilIdle()

            assertEquals(
                "Sydney",
                viewModel.shown.place
                    ?.title
                    ?.words,
            )
            assertEquals(ForecastState.Loading, viewModel.shown.forecast)

            weather.forecasts[1].complete(sampleForecast(temperature = 21.0))
            advanceUntilIdle()

            assertEquals("70°F", viewModel.temperature)
        }

    @Test
    fun `clearing the place goes back to the start and drops the forecast on its way`() =
        runTest {
            val viewModel = viewModel()
            weather.forecastToReturn = null
            viewModel.select(asheville)
            advanceUntilIdle()

            viewModel.clear()
            weather.forecasts[0].complete(sampleForecast())
            advanceUntilIdle()

            assertEquals(HomeState(units = UnitsUiModel.Fahrenheit), viewModel.shown)
        }

    @Test
    fun `choosing other units converts the forecast on screen without asking again and remembers the choice`() =
        runTest {
            val viewModel = viewModel()
            viewModel.select(asheville)
            advanceUntilIdle()
            val place = viewModel.shown.place
            assertEquals("36°F", viewModel.temperature)

            viewModel.setUnits(UnitsUiModel.Celsius)

            // At once, with no loading in between.
            assertEquals(UnitsUiModel.Celsius, viewModel.shown.units)
            assertEquals("2°C", viewModel.temperature)
            // The place is the same one, so the map has no reason to move.
            assertEquals(place, viewModel.shown.place)

            viewModel.setUnits(UnitsUiModel.Fahrenheit)
            advanceUntilIdle()

            assertEquals("36°F", viewModel.temperature)
            assertEquals(1, weather.forecastRequests.size)
            assertEquals(Units.Imperial, settings.units)
        }

    @Test
    fun `units chosen while the forecast is on its way are the ones it arrives in`() =
        runTest {
            val viewModel = viewModel()
            weather.forecastToReturn = null
            viewModel.select(asheville)
            advanceUntilIdle()

            viewModel.setUnits(UnitsUiModel.Celsius)

            assertEquals(UnitsUiModel.Celsius, viewModel.shown.units)
            assertEquals(ForecastState.Loading, viewModel.shown.forecast)

            weather.forecasts.single().complete(sampleForecast())
            advanceUntilIdle()

            assertEquals("2°C", viewModel.temperature)
            assertEquals(1, weather.forecastRequests.size)
        }

    @Test
    fun `choosing the units already shown does nothing`() =
        runTest {
            val viewModel = viewModel()
            viewModel.select(asheville)
            advanceUntilIdle()

            viewModel.setUnits(UnitsUiModel.Fahrenheit)
            advanceUntilIdle()

            assertEquals(1, weather.forecastRequests.size)
            assertNull(settings.units)
        }

    @Test
    fun `units can be chosen before any place is`() =
        runTest {
            val viewModel = viewModel()

            viewModel.setUnits(UnitsUiModel.Celsius)
            advanceUntilIdle()

            assertEquals(HomeState(units = UnitsUiModel.Celsius), viewModel.shown)
            assertEquals(emptyList(), weather.forecastRequests)
        }

    @Test
    fun `units that cannot be saved are still used for now`() =
        runTest {
            val viewModel = viewModel()
            settings.refusesToSave = true
            viewModel.select(asheville)
            advanceUntilIdle()

            viewModel.setUnits(UnitsUiModel.Celsius)
            advanceUntilIdle()

            assertEquals("2°C", viewModel.temperature)
            assertNull(settings.units)
        }

    @Test
    fun `there is nothing to try again with no place selected`() =
        runTest {
            val viewModel = viewModel()

            viewModel.retryForecast()
            advanceUntilIdle()

            assertEquals(emptyList(), weather.forecastRequests)
            assertEquals(ForecastState.Idle, viewModel.shown.forecast)
        }
}
