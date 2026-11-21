package com.calemccammon.weather.kmp.ui.core

import com.calemccammon.weather.kmp.domain.models.WeatherCondition
import com.calemccammon.weather.kmp.domain.repositories.WeatherFailure
import com.calemccammon.weather.kmp.testing.words
import kmp_weather.shared.generated.resources.Res
import kmp_weather.shared.generated.resources.speed_mph
import kmp_weather.shared.generated.resources.wind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class WordingTest {
    @Test
    fun `every failure has its own message`() {
        val messages = WeatherFailure.entries.map { it.message.words }

        assertEquals(WeatherFailure.entries.size, messages.toSet().size)
        assertEquals("The weather service took too long to answer.", WeatherFailure.Timeout.message.words)
        assertEquals("That point is not on the map.", WeatherFailure.InvalidPlace.message.words)
    }

    @Test
    fun `every condition has its own name`() {
        val names = WeatherCondition.entries.map { it.label.words }

        assertEquals(WeatherCondition.entries.size, names.toSet().size)
        assertEquals("Partly cloudy", WeatherCondition.PartlyCloudy.label.words)
        assertEquals("Freezing drizzle", WeatherCondition.FreezingDrizzle.label.words)
    }

    @Test
    fun `clear and partly cloudy skies are drawn differently at night`() {
        assertNotEquals(WeatherCondition.Clear.icon(isDay = true), WeatherCondition.Clear.icon(isDay = false))
        assertNotEquals(
            WeatherCondition.PartlyCloudy.icon(isDay = true),
            WeatherCondition.PartlyCloudy.icon(isDay = false),
        )
        assertEquals(WeatherCondition.Rain.icon(isDay = true), WeatherCondition.Rain.icon(isDay = false))
    }

    @Test
    fun `wording inside wording is looked up too`() {
        val text = uiText(Res.string.wind, uiText(Res.string.speed_mph, 4))

        assertEquals("Wind 4 mph", text.words)
        assertEquals("Asheville", UiText.Verbatim("Asheville").words)
    }
}
