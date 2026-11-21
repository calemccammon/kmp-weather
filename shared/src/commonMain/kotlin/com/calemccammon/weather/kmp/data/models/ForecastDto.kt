package com.calemccammon.weather.kmp.data.models

import com.calemccammon.weather.kmp.domain.models.CurrentConditions
import com.calemccammon.weather.kmp.domain.models.DailyForecast
import com.calemccammon.weather.kmp.domain.models.Forecast
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * The forecast response as Open-Meteo sends it: one `current` object and a
 * `daily` object of parallel columns.
 */
@Serializable
data class ForecastDto(
    val current: CurrentDto,
    val daily: DailyDto,
) {
    /**
     * Translates the wire shape into what the app works with: codes become
     * conditions, columns become one entry per day, and a day the weather
     * model has no temperatures for is left out.
     *
     * Throws an [IllegalArgumentException] if a date or time is not ISO 8601,
     * or the columns differ in length.
     */
    fun toDomain(): Forecast {
        require(daily.columns.all { it.size == daily.time.size }) { "daily columns differ in length" }

        return Forecast(
            current =
                CurrentConditions(
                    observedAt = LocalDateTime.parse(current.time),
                    temperature = current.temperature,
                    feelsLike = current.apparentTemperature,
                    windSpeed = current.windSpeed,
                    condition = weatherConditionFromWmo(current.weatherCode),
                    isDay = current.isDay == 1,
                ),
            days =
                daily.time.indices.mapNotNull { i ->
                    val high = daily.temperatureMax[i] ?: return@mapNotNull null
                    val low = daily.temperatureMin[i] ?: return@mapNotNull null
                    DailyForecast(
                        date = LocalDate.parse(daily.time[i]),
                        high = high,
                        low = low,
                        condition = weatherConditionFromWmo(daily.weatherCode[i]),
                        precipitationChance = daily.precipitationProbabilityMax?.get(i)?.roundToInt(),
                    )
                },
        )
    }
}

/** The response's `current` object. */
@Serializable
data class CurrentDto(
    /** ISO date and time, local to the place: `2026-11-18T09:00`. */
    val time: String,
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("apparent_temperature") val apparentTemperature: Double,
    @SerialName("weather_code") val weatherCode: Int,
    @SerialName("wind_speed_10m") val windSpeed: Double,
    /** 1 in daylight, 0 at night. */
    @SerialName("is_day") val isDay: Int,
)

/**
 * The response's `daily` object: a list of dates and one list per variable,
 * all the same length. A value the weather model lacks is null.
 */
@Serializable
data class DailyDto(
    /** ISO dates, local to the place: `2026-11-18`. */
    val time: List<String>,
    @SerialName("weather_code") val weatherCode: List<Int?>,
    @SerialName("temperature_2m_max") val temperatureMax: List<Double?>,
    @SerialName("temperature_2m_min") val temperatureMin: List<Double?>,
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Double?>? = null,
) {
    /** Every column but the dates. */
    val columns: List<List<*>>
        get() = listOfNotNull(weatherCode, temperatureMax, temperatureMin, precipitationProbabilityMax)
}
