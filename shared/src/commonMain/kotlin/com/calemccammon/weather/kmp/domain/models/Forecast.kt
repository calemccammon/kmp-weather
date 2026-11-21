package com.calemccammon.weather.kmp.domain.models

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/** The weather right now at a place. */
data class CurrentConditions(
    /**
     * When these conditions were measured, on the place's own clock. It says
     * which calendar day is "today" there, whatever the device's clock says.
     */
    val observedAt: LocalDateTime,
    val temperature: Double,
    val feelsLike: Double,
    val windSpeed: Double,
    val condition: WeatherCondition,
    val isDay: Boolean,
)

/** One day of the weekly forecast. */
data class DailyForecast(
    /** The calendar day at the place. */
    val date: LocalDate,
    val low: Double,
    val high: Double,
    val condition: WeatherCondition,
    /** Highest chance of precipitation during the day, in percent. */
    val precipitationChance: Int? = null,
)

/** Current conditions and the days ahead for one place. */
data class Forecast(
    val current: CurrentConditions,
    val days: List<DailyForecast>,
) {
    /** Whether [day] is the day the current conditions were measured on. */
    fun isToday(day: DailyForecast): Boolean = day.date == current.observedAt.date
}
