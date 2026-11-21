package com.calemccammon.weather.kmp.data.models

import com.calemccammon.weather.kmp.domain.models.WeatherCondition
import kotlin.test.Test
import kotlin.test.assertEquals

class WmoCodeTest {
    @Test
    fun `each group of codes is its condition`() {
        val expected =
            mapOf(
                0 to WeatherCondition.Clear,
                1 to WeatherCondition.MostlyClear,
                2 to WeatherCondition.PartlyCloudy,
                3 to WeatherCondition.Overcast,
                48 to WeatherCondition.Fog,
                53 to WeatherCondition.Drizzle,
                57 to WeatherCondition.FreezingDrizzle,
                65 to WeatherCondition.Rain,
                66 to WeatherCondition.FreezingRain,
                77 to WeatherCondition.Snow,
                81 to WeatherCondition.RainShowers,
                86 to WeatherCondition.SnowShowers,
                99 to WeatherCondition.Thunderstorm,
            )

        assertEquals(expected, expected.mapValues { weatherConditionFromWmo(it.key) })
    }

    @Test
    fun `a code outside the table or a missing one is unknown`() {
        assertEquals(WeatherCondition.Unknown, weatherConditionFromWmo(4))
        assertEquals(WeatherCondition.Unknown, weatherConditionFromWmo(-1))
        assertEquals(WeatherCondition.Unknown, weatherConditionFromWmo(null))
    }
}
