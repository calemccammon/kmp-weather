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

/**
 * Current conditions and the days ahead for one place.
 *
 * A forecast from the repository is metric: degrees Celsius and kilometres an
 * hour. [inUnits] gives the same forecast in another system, so changing units
 * never needs the network.
 */
data class Forecast(
    val current: CurrentConditions,
    val days: List<DailyForecast>,
) {
    /** Whether [day] is the day the current conditions were measured on. */
    fun isToday(day: DailyForecast): Boolean = day.date == current.observedAt.date

    /**
     * This metric forecast with its temperatures and wind speed in [units].
     * The chance of precipitation is a percentage in any units.
     */
    fun inUnits(units: Units): Forecast =
        when (units) {
            Units.Metric -> this
            Units.Imperial ->
                Forecast(
                    current =
                        current.copy(
                            temperature = fahrenheit(current.temperature),
                            feelsLike = fahrenheit(current.feelsLike),
                            windSpeed = current.windSpeed * MILES_PER_KILOMETRE,
                        ),
                    days = days.map { it.copy(low = fahrenheit(it.low), high = fahrenheit(it.high)) },
                )
        }

    private companion object {
        const val MILES_PER_KILOMETRE = 0.621371
        const val FAHRENHEIT_PER_CELSIUS = 1.8
        const val FREEZING_FAHRENHEIT = 32

        fun fahrenheit(celsius: Double): Double = celsius * FAHRENHEIT_PER_CELSIUS + FREEZING_FAHRENHEIT
    }
}
