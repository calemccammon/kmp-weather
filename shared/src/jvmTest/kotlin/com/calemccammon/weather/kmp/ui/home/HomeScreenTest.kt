package com.calemccammon.weather.kmp.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
import com.calemccammon.weather.kmp.testing.MainDispatcherTest
import com.calemccammon.weather.kmp.testing.asheville
import com.calemccammon.weather.kmp.testing.sampleForecast
import com.calemccammon.weather.kmp.ui.core.WeatherTheme
import com.calemccammon.weather.kmp.ui.home.components.MapPin
import com.calemccammon.weather.kmp.ui.home.viewmodels.HomeViewModel
import com.calemccammon.weather.kmp.ui.home.viewmodels.SearchViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The whole screen over a repository that answers from memory, drawn in a
 * desktop window of a chosen size. The real map is native code; the one here
 * records what the screen asks of it.
 */
@OptIn(ExperimentalTestApi::class)
class HomeScreenTest : MainDispatcherTest() {
    private val weather = FakeWeatherRepository().apply { placesToReturn = listOf(asheville) }
    private val settings = FakeSettingsRepository()
    private val home by lazy {
        HomeViewModel(GetForecast(weather), GetUnits(settings), SetUnits(settings), deviceCountry = "US")
    }
    private val search by lazy { SearchViewModel(SearchPlaces(weather)) }

    // What the screen last asked of the map.
    private var pin: MapPin? = null
    private var covered = PaddingValues()
    private var longPress: (Double, Double) -> Unit = { _, _ -> }

    private fun onScreen(
        width: Int = WIDE.first,
        height: Int = WIDE.second,
        test: DesktopComposeUiTest.() -> Unit,
    ) = runDesktopComposeUiTest(width, height) {
        setContent {
            WeatherTheme(dark = false) {
                HomeScreen(home, search) { pin, covered, onLongPress ->
                    this@HomeScreenTest.pin = pin
                    this@HomeScreenTest.covered = covered
                    longPress = onLongPress
                }
            }
        }
        // The test moves the screen's clock itself. Left to run, it would
        // wait for the "loading" bars to stop moving, which they never do.
        mainClock.autoAdvance = false
        settle()
        test()
    }

    /**
     * Moves time on: long enough for a search's pause, for an answer to come
     * back and be drawn, and for Material's animations to play out. The view
     * models and the screen are on the one test clock.
     */
    private fun DesktopComposeUiTest.settle() = mainClock.advanceTimeBy(SETTLE_MS)

    private fun DesktopComposeUiTest.select() {
        home.select(asheville)
        settle()
    }

    private fun DesktopComposeUiTest.type(text: String) {
        searchBar().performClick()
        settle()
        searchBar().performTextInput(text)
        settle()
    }

    private fun DesktopComposeUiTest.searchBar() = onNode(hasSetTextAction())

    /**
     * Text inside a block that a screen reader is given as one sentence: it is
     * drawn, but it is not what is read out.
     */
    private fun DesktopComposeUiTest.onDrawnText(text: String) = onNodeWithText(text, useUnmergedTree = true)

    /** Whatever a screen reader reads out as it changes. */
    private val announced =
        hasAnyAncestor(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))

    @Test
    fun `it starts with a hint and no forecast`() =
        onScreen {
            onNode(hasText("Search for a place, or press and hold", substring = true) and announced).assertExists()
            onNodeWithText("This week").assertDoesNotExist()
            assertNull(pin)
        }

    // Search.

    @Test
    fun `a place that is searched for and chosen shows its week`() =
        onScreen {
            type("ashev")
            onNodeWithText("North Carolina, United States").performClick()
            settle()

            onNodeWithText("This week").assertExists()
            onDrawnText("Today").assertExists()
            onDrawnText("36°F").assertExists()
            onDrawnText("Feels like 31°").assertExists()
            onDrawnText("Wind 4 mph").assertExists()
            onDrawnText("80%").assertExists()
            onNodeWithText("Weather data by Open-Meteo.com").assertExists()
            searchBar().assertTextEquals("Asheville")
            assertEquals(listOf("ashev"), weather.searchQueries)
            assertEquals(MapPin(35.6, -82.55), pin)
        }

    @Test
    fun `the open search bar asks for something to type`() =
        onScreen {
            searchBar().performClick()
            settle()

            onNode(hasText("Type a city, town or postcode") and announced).assertExists()
        }

    @Test
    fun `a search that matches nothing says so`() =
        onScreen {
            weather.placesToReturn = emptyList()

            type("zzzz")

            onNode(hasText("Nothing found for \"zzzz\"") and announced).assertExists()
        }

    @Test
    fun `a search that fails says what went wrong and finds the place on another try`() =
        onScreen {
            weather.failure = WeatherException(WeatherFailure.NoConnection)

            type("ashev")
            onNode(hasText("Could not reach the weather service. Check your connection.") and announced).assertExists()

            weather.failure = null
            onNodeWithText("Try again").performClick()
            settle()

            onNodeWithText("North Carolina, United States").assertExists()
        }

    @Test
    fun `the name of the chosen place is not searched for when the bar opens`() =
        onScreen {
            longPress(35.6, -82.55)
            settle()
            searchBar().assertTextEquals("35.60°N, 82.55°W")

            searchBar().performClick()
            settle()

            onNodeWithText("Type a city, town or postcode").assertExists()
            onNodeWithText("35.60°N, 82.55°W", useUnmergedTree = true).assertExists() // Still the forecast's heading.
            assertEquals(emptyList(), weather.searchQueries)
        }

    @Test
    fun `opening the bar again starts a new search`() =
        onScreen {
            type("ashev")
            onNodeWithText("North Carolina, United States").performClick()
            settle()

            searchBar().performClick()
            settle()

            onNodeWithText("Type a city, town or postcode").assertExists()
            searchBar().assertTextEquals("Search for a place", includeEditableText = false)
        }

    // Forecast.

    @Test
    fun `a forecast that fails offers another try and shows it is working and then the week`() =
        onScreen {
            weather.forecastToReturn = null
            select()
            weather.forecasts[0].completeExceptionally(WeatherException(WeatherFailure.Timeout))
            settle()

            onNode(hasText("The weather service took too long to answer.") and announced).assertExists()
            onNodeWithText("This week").assertDoesNotExist()

            onNodeWithText("Try again").performClick()
            settle()

            onNode(hasContentDescription("Loading the forecast") and announced).assertExists()
            onNodeWithText("The weather service took too long to answer.").assertDoesNotExist()

            weather.forecasts[1].complete(sampleForecast())
            settle()

            onNodeWithText("This week").assertExists()
        }

    @Test
    fun `clearing the place brings the hint back`() =
        onScreen {
            select()
            onNodeWithText("This week").assertExists()

            onNodeWithContentDescription("Clear place").performClick()
            settle()

            onNodeWithText("Search for a place, or press and hold", substring = true).assertExists()
            onNodeWithText("This week").assertDoesNotExist()
            assertNull(pin)
            // Clearing is not searching: the results stay shut.
            onNodeWithText("Type a city, town or postcode").assertDoesNotExist()
        }

    @Test
    fun `each block is read out as one sentence`() =
        onScreen {
            select()

            onNodeWithContentDescription("Now: Clear, 36 degrees Fahrenheit, feels like 31, wind 4 mph").assertExists()
            onNodeWithContentDescription(
                "Sunday: Rain, low 29, high 64 degrees, 80 percent chance of precipitation",
            ).assertExists()
        }

    @Test
    fun `choosing Celsius shows the forecast in it and remembers the choice`() =
        onScreen {
            select()
            onNodeWithContentDescription("Fahrenheit and mph").assertIsSelected()

            onNodeWithContentDescription("Celsius and km/h").performClick()
            settle()

            onDrawnText("36°C").assertExists()
            onDrawnText("Wind 4 km/h").assertExists()
            onNodeWithContentDescription("Celsius and km/h").assertIsSelected()
            assertEquals(Units.Metric, weather.forecastRequests.last().units)
            assertEquals(Units.Metric, settings.units)
        }

    @Test
    fun `the units choice is tall enough to press`() =
        onScreen {
            select()

            // Material draws a segment 40 high and takes presses over 48.
            onNodeWithContentDescription("Celsius and km/h").assertTouchHeightIsEqualTo(48.dp)
        }

    // Map.

    @Test
    fun `a press and hold on the map picks the point under it`() =
        onScreen {
            longPress(35.6, -82.55)
            settle()

            onNodeWithText("This week").assertExists()
            assertEquals(listOf(ForecastRequest(35.6, -82.55, Units.Imperial)), weather.forecastRequests)
            assertEquals(MapPin(35.6, -82.55), pin)
        }

    @Test
    fun `on a wide window the map is told the side panel covers its left`() =
        onScreen(WIDE.first, WIDE.second) {
            select()

            assertEquals(432.dp, covered.calculateLeftPadding(LayoutDirection.Ltr))
            assertEquals(0.dp, covered.calculateTopPadding())
            assertEquals(0.dp, covered.calculateBottomPadding())
        }

    @Test
    fun `on a phone the map is told the search bar and the sheet cover its top and bottom`() =
        onScreen(PHONE.first, PHONE.second) {
            select()

            assertEquals(84.dp, covered.calculateTopPadding())
            assertEquals(PHONE.second.dp * 0.3f, covered.calculateBottomPadding())
            assertEquals(0.dp, covered.calculateLeftPadding(LayoutDirection.Ltr))
        }

    // Layout.

    @Test
    fun `a phone-sized window shows the forecast in a sheet that can be dragged`() =
        onScreen(PHONE.first, PHONE.second) {
            select()

            onNodeWithContentDescription("Drag handle").assertExists()
            onDrawnText("36°F").assertExists()
        }

    @Test
    fun `a wide window shows the forecast in a side panel with no sheet`() =
        onScreen(WIDE.first, WIDE.second) {
            select()

            onNodeWithContentDescription("Drag handle").assertDoesNotExist()
            onDrawnText("36°F").assertExists()
        }

    private companion object {
        val WIDE = 1200 to 900
        val PHONE = 400 to 800

        /** Longer than the pause before a search, and than any of Material's animations. */
        const val SETTLE_MS = 1_000L
    }
}
