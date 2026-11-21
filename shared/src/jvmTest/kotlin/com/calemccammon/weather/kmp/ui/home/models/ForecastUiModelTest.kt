package com.calemccammon.weather.kmp.ui.home.models

import com.calemccammon.weather.kmp.domain.models.DailyForecast
import com.calemccammon.weather.kmp.domain.models.Units
import com.calemccammon.weather.kmp.domain.models.WeatherCondition
import com.calemccammon.weather.kmp.testing.sampleForecast
import com.calemccammon.weather.kmp.testing.words
import com.calemccammon.weather.kmp.ui.core.icon
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ForecastUiModelTest {
    private val imperial = ForecastUiModel.from(sampleForecast(), Units.Imperial)

    @Test
    fun `current conditions are rounded and worded in imperial units`() {
        val current = imperial.current

        assertEquals("36°F", current.temperature.words)
        assertEquals("Clear", current.condition.words)
        assertEquals("Feels like 31°", current.feelsLike.words)
        assertEquals("Wind 4 mph", current.wind.words)
        assertEquals(WeatherCondition.Clear.icon(isDay = true), current.icon)
        assertEquals("Now: Clear, 36 degrees Fahrenheit, feels like 31, wind 4 mph", current.description.words)
    }

    @Test
    fun `current conditions are worded in metric units`() {
        val current = ForecastUiModel.from(sampleForecast(temperature = 2.4), Units.Metric).current

        assertEquals("2°C", current.temperature.words)
        assertEquals("Wind 4 km/h", current.wind.words)
        assertEquals("Now: Clear, 2 degrees Celsius, feels like 31, wind 4 km/h", current.description.words)
    }

    @Test
    fun `the day the forecast was made on is today and the rest are named`() {
        assertEquals(listOf("Today", "Thu", "Sun"), imperial.days.map { it.dayLabel.words })
    }

    @Test
    fun `a day shows its rounded low and high and its chance of precipitation`() {
        val (today, thursday) = imperial.days

        assertEquals(listOf("30°", "64°"), listOf(today.low.words, today.high.words))
        assertNull(today.chance)
        assertEquals("47%", thursday.chance?.words)
        assertEquals(WeatherCondition.Rain.icon(), thursday.icon)
    }

    @Test
    fun `a day is one sentence for a screen reader`() {
        val (today, thursday) = imperial.days

        assertEquals("Today: Clear, low 30, high 64 degrees", today.description.words)
        assertEquals(
            "Thursday: Rain, low 42, high 58 degrees, 47 percent chance of precipitation",
            thursday.description.words,
        )
    }

    @Test
    fun `a day's bar shows where its range sits in the week's`() {
        // The week runs from 28.6 to 64.3.
        val (today, thursday, sunday) = imperial.days

        assertEquals(1f, today.barEnd)
        assertEquals((30.2f - 28.6f) / (64.3f - 28.6f), today.barStart, absoluteTolerance = 0.0001f)
        assertEquals((58.1f - 28.6f) / (64.3f - 28.6f), thursday.barEnd, absoluteTolerance = 0.0001f)
        assertEquals(0f to 1f, sunday.barStart to sunday.barEnd)
    }

    @Test
    fun `a week of one temperature fills every bar`() {
        val flat = DailyForecast(LocalDate(2026, 11, 18), low = 50.0, high = 50.0, condition = WeatherCondition.Fog)

        val day = ForecastUiModel.from(sampleForecast(days = listOf(flat)), Units.Imperial).days.single()

        assertEquals(0f to 1f, day.barStart to day.barEnd)
    }

    @Test
    fun `a forecast with no days has none to show`() {
        assertEquals(emptyList(), ForecastUiModel.from(sampleForecast(days = emptyList()), Units.Metric).days)
    }
}
