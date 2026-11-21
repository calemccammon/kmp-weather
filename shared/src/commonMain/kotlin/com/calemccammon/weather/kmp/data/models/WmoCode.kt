package com.calemccammon.weather.kmp.data.models

import com.calemccammon.weather.kmp.domain.models.WeatherCondition

/**
 * What a WMO weather code, as Open-Meteo reports it, means.
 *
 * The table is on https://open-meteo.com/en/docs under "WMO Weather
 * interpretation codes". A code outside it is [WeatherCondition.Unknown].
 */
@Suppress("MagicNumber", "CyclomaticComplexMethod") // The codes are the table.
fun weatherConditionFromWmo(code: Int?): WeatherCondition =
    when (code) {
        0 -> WeatherCondition.Clear
        1 -> WeatherCondition.MostlyClear
        2 -> WeatherCondition.PartlyCloudy
        3 -> WeatherCondition.Overcast
        45, 48 -> WeatherCondition.Fog
        51, 53, 55 -> WeatherCondition.Drizzle
        56, 57 -> WeatherCondition.FreezingDrizzle
        61, 63, 65 -> WeatherCondition.Rain
        66, 67 -> WeatherCondition.FreezingRain
        71, 73, 75, 77 -> WeatherCondition.Snow
        80, 81, 82 -> WeatherCondition.RainShowers
        85, 86 -> WeatherCondition.SnowShowers
        95, 96, 99 -> WeatherCondition.Thunderstorm
        else -> WeatherCondition.Unknown
    }
