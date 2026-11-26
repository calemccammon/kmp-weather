package com.calemccammon.weather.kmp.ui.home.models

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.calemccammon.weather.kmp.domain.models.CurrentConditions
import com.calemccammon.weather.kmp.domain.models.DailyForecast
import com.calemccammon.weather.kmp.domain.models.Forecast
import com.calemccammon.weather.kmp.domain.models.Units
import com.calemccammon.weather.kmp.ui.core.UiText
import com.calemccammon.weather.kmp.ui.core.icon
import com.calemccammon.weather.kmp.ui.core.label
import com.calemccammon.weather.kmp.ui.core.uiText
import kmp_weather.shared.generated.resources.Res
import kmp_weather.shared.generated.resources.current_description
import kmp_weather.shared.generated.resources.day_description
import kmp_weather.shared.generated.resources.day_description_with_chance
import kmp_weather.shared.generated.resources.day_friday
import kmp_weather.shared.generated.resources.day_monday
import kmp_weather.shared.generated.resources.day_saturday
import kmp_weather.shared.generated.resources.day_short_friday
import kmp_weather.shared.generated.resources.day_short_monday
import kmp_weather.shared.generated.resources.day_short_saturday
import kmp_weather.shared.generated.resources.day_short_sunday
import kmp_weather.shared.generated.resources.day_short_thursday
import kmp_weather.shared.generated.resources.day_short_tuesday
import kmp_weather.shared.generated.resources.day_short_wednesday
import kmp_weather.shared.generated.resources.day_sunday
import kmp_weather.shared.generated.resources.day_thursday
import kmp_weather.shared.generated.resources.day_tuesday
import kmp_weather.shared.generated.resources.day_wednesday
import kmp_weather.shared.generated.resources.feels_like
import kmp_weather.shared.generated.resources.precipitation_chance
import kmp_weather.shared.generated.resources.speed_kmh
import kmp_weather.shared.generated.resources.speed_mph
import kmp_weather.shared.generated.resources.spoken_celsius
import kmp_weather.shared.generated.resources.spoken_fahrenheit
import kmp_weather.shared.generated.resources.temperature
import kmp_weather.shared.generated.resources.temperature_celsius
import kmp_weather.shared.generated.resources.temperature_fahrenheit
import kmp_weather.shared.generated.resources.today
import kmp_weather.shared.generated.resources.wind
import kotlinx.datetime.DayOfWeek
import kotlin.math.roundToInt

/**
 * The forecast as the screen shows it: every value already rounded, worded
 * and laid out, so the composables only place it.
 */
@Immutable
data class ForecastUiModel(
    val current: CurrentUiModel,
    val days: List<DayUiModel>,
) {
    companion object {
        /**
         * Translates a metric domain [Forecast] for display in [units]. The
         * values are converted before they are rounded.
         */
        fun from(
            metric: Forecast,
            units: Units,
        ): ForecastUiModel {
            val forecast = metric.inUnits(units)
            // The whole week's range, so each day's bar shows where it sits in it.
            val weekLow = forecast.days.minOfOrNull { it.low } ?: 0.0
            val weekHigh = forecast.days.maxOfOrNull { it.high } ?: 0.0

            return ForecastUiModel(
                current = CurrentUiModel.from(forecast.current, units),
                days = forecast.days.map { DayUiModel.from(it, forecast.isToday(it), weekLow, weekHigh) },
            )
        }
    }
}

/** The "right now" block at the top of the forecast. */
@Immutable
data class CurrentUiModel(
    val icon: ImageVector,
    /** "34°F". The one place the scale is spelled out; the rest of the forecast is in the same one. */
    val temperature: UiText,
    /** "Overcast". */
    val condition: UiText,
    /** "Feels like 28°". */
    val feelsLike: UiText,
    /** "Wind 2 mph". */
    val wind: UiText,
    /** The whole block as one sentence for a screen reader. */
    val description: UiText,
) {
    companion object {
        fun from(
            current: CurrentConditions,
            units: Units,
        ): CurrentUiModel {
            val temperature = current.temperature.roundToInt()
            val feelsLike = current.feelsLike.roundToInt()
            val wind = current.windSpeed.roundToInt()
            val (shown, spoken, speed) =
                when (units) {
                    Units.Metric ->
                        Triple(
                            uiText(Res.string.temperature_celsius, temperature),
                            uiText(Res.string.spoken_celsius, temperature),
                            uiText(Res.string.speed_kmh, wind),
                        )
                    Units.Imperial ->
                        Triple(
                            uiText(Res.string.temperature_fahrenheit, temperature),
                            uiText(Res.string.spoken_fahrenheit, temperature),
                            uiText(Res.string.speed_mph, wind),
                        )
                }
            val condition = current.condition.label

            return CurrentUiModel(
                icon = current.condition.icon(isDay = current.isDay),
                temperature = shown,
                condition = condition,
                feelsLike = uiText(Res.string.feels_like, feelsLike),
                wind = uiText(Res.string.wind, speed),
                description = uiText(Res.string.current_description, condition, spoken, feelsLike, speed),
            )
        }
    }
}

/** One row of the week. */
@Immutable
data class DayUiModel(
    /** "Today" for the day the forecast was made on, otherwise a short weekday: "Sat". */
    val dayLabel: UiText,
    val icon: ImageVector,
    /** "47%", or null when there is no chance worth showing. */
    val chance: UiText?,
    /** "30°". */
    val low: UiText,
    /** "68°". */
    val high: UiText,
    /**
     * Where this day's low and high fall within the week's range, each from 0
     * (the week's lowest) to 1 (its highest).
     */
    val barStart: Float,
    val barEnd: Float,
    /** The whole row as one sentence for a screen reader. */
    val description: UiText,
) {
    companion object {
        fun from(
            day: DailyForecast,
            isToday: Boolean,
            weekLow: Double,
            weekHigh: Double,
        ): DayUiModel {
            val low = day.low.roundToInt()
            val high = day.high.roundToInt()
            val chance = day.precipitationChance ?: 0
            val span = weekHigh - weekLow
            val today = uiText(Res.string.today)
            val dayName = if (isToday) today else uiText(day.date.dayOfWeek.fullName)
            val condition = day.condition.label

            return DayUiModel(
                dayLabel = if (isToday) today else uiText(day.date.dayOfWeek.shortName),
                icon = day.condition.icon(),
                chance = if (chance > 0) uiText(Res.string.precipitation_chance, chance) else null,
                low = uiText(Res.string.temperature, low),
                high = uiText(Res.string.temperature, high),
                // A week with one temperature throughout fills the whole bar.
                barStart = if (span <= 0) 0f else ((day.low - weekLow) / span).toFloat(),
                barEnd = if (span <= 0) 1f else ((day.high - weekLow) / span).toFloat(),
                description =
                    if (chance > 0) {
                        uiText(Res.string.day_description_with_chance, dayName, condition, low, high, chance)
                    } else {
                        uiText(Res.string.day_description, dayName, condition, low, high)
                    },
            )
        }

        private val DayOfWeek.shortName
            get() =
                when (this) {
                    DayOfWeek.MONDAY -> Res.string.day_short_monday
                    DayOfWeek.TUESDAY -> Res.string.day_short_tuesday
                    DayOfWeek.WEDNESDAY -> Res.string.day_short_wednesday
                    DayOfWeek.THURSDAY -> Res.string.day_short_thursday
                    DayOfWeek.FRIDAY -> Res.string.day_short_friday
                    DayOfWeek.SATURDAY -> Res.string.day_short_saturday
                    DayOfWeek.SUNDAY -> Res.string.day_short_sunday
                }

        private val DayOfWeek.fullName
            get() =
                when (this) {
                    DayOfWeek.MONDAY -> Res.string.day_monday
                    DayOfWeek.TUESDAY -> Res.string.day_tuesday
                    DayOfWeek.WEDNESDAY -> Res.string.day_wednesday
                    DayOfWeek.THURSDAY -> Res.string.day_thursday
                    DayOfWeek.FRIDAY -> Res.string.day_friday
                    DayOfWeek.SATURDAY -> Res.string.day_saturday
                    DayOfWeek.SUNDAY -> Res.string.day_sunday
                }
    }
}
