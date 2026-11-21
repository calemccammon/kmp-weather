package com.calemccammon.weather.kmp.di

import com.calemccammon.weather.kmp.domain.models.Units
import com.calemccammon.weather.kmp.domain.repositories.SettingsRepository
import com.calemccammon.weather.kmp.domain.repositories.WeatherRepository
import com.calemccammon.weather.kmp.testing.FakeSettingsRepository
import com.calemccammon.weather.kmp.testing.FakeWeatherRepository
import com.calemccammon.weather.kmp.testing.MainDispatcherTest
import com.calemccammon.weather.kmp.testing.asheville
import com.calemccammon.weather.kmp.ui.home.models.ForecastState
import com.calemccammon.weather.kmp.ui.home.models.UnitsUiModel
import com.calemccammon.weather.kmp.ui.home.viewmodels.HomeViewModel
import com.calemccammon.weather.kmp.ui.home.viewmodels.SearchViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class DependenciesTest : MainDispatcherTest() {
    private val weather = FakeWeatherRepository().apply { placesToReturn = listOf(asheville) }
    private val settings = FakeSettingsRepository(Units.Metric)
    private val fakes =
        module {
            single<WeatherRepository> { weather }
            single<SettingsRepository> { settings }
        }

    @Test
    fun `the view models are built over whichever repositories are supplied`() =
        runTest {
            val koin = koinApplication { modules(fakes, appModule) }.koin
            val home = koin.get<HomeViewModel>()
            val search = koin.get<SearchViewModel>()

            assertEquals(UnitsUiModel.Celsius, home.state.value.units)

            search.queryChanged("ashev")
            advanceUntilIdle()
            home.select(assertNotNull(search.resultAt(0)))
            advanceUntilIdle()

            assertIs<ForecastState.Loaded>(home.state.value.forecast)
            assertEquals(listOf("ashev"), weather.searchQueries)
        }

    @Test
    fun `the real repositories can be built`() {
        val koin = koinApplication { modules(repositoriesModule, appModule) }.koin

        assertNotNull(koin.get<WeatherRepository>())
        assertNotNull(koin.get<SettingsRepository>())
    }
}
