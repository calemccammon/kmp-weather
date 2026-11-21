package com.calemccammon.weather.kmp.ui.home.viewmodels

import com.calemccammon.weather.kmp.domain.repositories.WeatherException
import com.calemccammon.weather.kmp.domain.repositories.WeatherFailure
import com.calemccammon.weather.kmp.domain.usecases.SearchPlaces
import com.calemccammon.weather.kmp.testing.FakeWeatherRepository
import com.calemccammon.weather.kmp.testing.MainDispatcherTest
import com.calemccammon.weather.kmp.testing.asheville
import com.calemccammon.weather.kmp.testing.sydney
import com.calemccammon.weather.kmp.testing.words
import com.calemccammon.weather.kmp.ui.home.models.SearchState
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds

class SearchViewModelTest : MainDispatcherTest() {
    private val weather = FakeWeatherRepository().apply { placesToReturn = listOf(asheville, sydney) }
    private val viewModel by lazy { SearchViewModel(SearchPlaces(weather)) }

    private val shown: SearchState
        get() = viewModel.state.value

    @Test
    fun `it starts with nothing to show`() {
        assertEquals(SearchState.Idle, shown)
    }

    @Test
    fun `too little to search for is not searched for`() =
        runTest {
            viewModel.queryChanged(" a ")
            advanceUntilIdle()

            assertEquals(SearchState.Idle, shown)
            assertEquals(emptyList(), weather.searchQueries)
        }

    @Test
    fun `it waits for a pause in typing before it searches`() =
        runTest {
            viewModel.queryChanged("ashev")

            assertEquals(SearchState.Loading("ashev"), shown)
            advanceTimeBy(SearchViewModel.debounce - 1.milliseconds)
            assertEquals(emptyList(), weather.searchQueries)

            advanceTimeBy(1.milliseconds)
            runCurrent()

            assertEquals(listOf("ashev"), weather.searchQueries)
        }

    @Test
    fun `typing on starts the wait again so only the last text is searched for`() =
        runTest {
            viewModel.queryChanged("as")
            advanceTimeBy(300.milliseconds)
            viewModel.queryChanged("ashev")
            advanceTimeBy(300.milliseconds)

            assertEquals(emptyList(), weather.searchQueries)

            advanceUntilIdle()

            assertEquals(listOf("ashev"), weather.searchQueries)
        }

    @Test
    fun `the places found are shown as rows and each row gives its place back`() =
        runTest {
            viewModel.queryChanged("a place")
            advanceUntilIdle()

            val results = assertIs<SearchState.Results>(shown)
            assertEquals("a place", results.query)
            assertEquals(listOf("Asheville", "Sydney"), results.places.map { it.title.words })
            assertEquals("North Carolina, United States", results.places.first().subtitle)
            assertEquals(sydney, viewModel.resultAt(1))
            assertNull(viewModel.resultAt(2))
            assertNull(viewModel.resultAt(-1))
        }

    @Test
    fun `finding nothing is said as such`() =
        runTest {
            weather.placesToReturn = emptyList()

            viewModel.queryChanged("zzzz")
            advanceUntilIdle()

            assertEquals(SearchState.Empty("zzzz"), shown)
            assertNull(viewModel.resultAt(0))
        }

    @Test
    fun `a failure is worded for the user and another try searches at once`() =
        runTest {
            weather.failure = WeatherException(WeatherFailure.NoConnection)

            viewModel.queryChanged("ashev")
            advanceUntilIdle()

            val failed = assertIs<SearchState.Failed>(shown)
            assertEquals("Could not reach the weather service. Check your connection.", failed.message.words)

            weather.failure = null
            viewModel.retry()

            assertEquals(SearchState.Loading("ashev"), shown)
            runCurrent()
            assertIs<SearchState.Results>(shown)
            assertEquals(listOf("ashev", "ashev"), weather.searchQueries)
        }

    @Test
    fun `the answer to an older search never replaces a newer one`() =
        runTest {
            weather.placesToReturn = null

            viewModel.queryChanged("ashev")
            advanceUntilIdle()
            viewModel.queryChanged("sydney")
            advanceUntilIdle()
            weather.searches[0].complete(listOf(asheville))
            advanceUntilIdle()

            assertEquals(SearchState.Loading("sydney"), shown)

            weather.searches[1].complete(listOf(sydney))
            advanceUntilIdle()

            assertEquals(listOf("Sydney"), assertIs<SearchState.Results>(shown).places.map { it.title.words })
            assertEquals(sydney, viewModel.resultAt(0))
        }

    @Test
    fun `the same text typed again is not searched for twice`() =
        runTest {
            viewModel.queryChanged("ashev")
            advanceUntilIdle()
            viewModel.queryChanged(" ashev ")
            advanceUntilIdle()

            assertEquals(listOf("ashev"), weather.searchQueries)
        }

    @Test
    fun `resetting forgets the text and the results and drops a search on its way`() =
        runTest {
            viewModel.queryChanged("ashev")
            advanceUntilIdle()
            weather.placesToReturn = null
            viewModel.queryChanged("sydney")
            advanceUntilIdle()

            viewModel.reset()
            weather.searches[0].complete(listOf(sydney))
            advanceUntilIdle()

            assertEquals(SearchState.Idle, shown)
            assertNull(viewModel.resultAt(0))

            // The text that was there before can be searched for again.
            weather.placesToReturn = listOf(sydney)
            viewModel.queryChanged("sydney")
            advanceUntilIdle()

            assertIs<SearchState.Results>(shown)
        }

    @Test
    fun `there is nothing to try again before anything is typed`() =
        runTest {
            viewModel.retry()
            advanceUntilIdle()

            assertEquals(SearchState.Idle, shown)
            assertEquals(emptyList(), weather.searchQueries)
        }
}
