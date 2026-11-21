package com.calemccammon.weather.kmp.testing

import com.calemccammon.weather.kmp.domain.models.CurrentConditions
import com.calemccammon.weather.kmp.domain.models.DailyForecast
import com.calemccammon.weather.kmp.domain.models.Forecast
import com.calemccammon.weather.kmp.domain.models.Place
import com.calemccammon.weather.kmp.domain.models.Units
import com.calemccammon.weather.kmp.domain.models.WeatherCondition
import com.calemccammon.weather.kmp.domain.repositories.SettingsException
import com.calemccammon.weather.kmp.domain.repositories.SettingsRepository
import com.calemccammon.weather.kmp.domain.repositories.WeatherException
import com.calemccammon.weather.kmp.domain.repositories.WeatherRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

val asheville =
    Place(
        name = "Asheville",
        latitude = 35.6,
        longitude = -82.55,
        region = "North Carolina",
        country = "United States",
    )

val sydney = Place(name = "Sydney", latitude = -33.87, longitude = 151.21, country = "Australia")

/** A Wednesday morning, then a week whose first day is that Wednesday. */
fun sampleForecast(
    temperature: Double = 35.8,
    days: List<DailyForecast> =
        listOf(
            DailyForecast(LocalDate(2026, 11, 18), low = 30.2, high = 64.3, condition = WeatherCondition.Clear),
            DailyForecast(
                LocalDate(2026, 11, 19),
                low = 41.9,
                high = 58.1,
                condition = WeatherCondition.Rain,
                precipitationChance = 47,
            ),
            DailyForecast(
                LocalDate(2026, 11, 22),
                low = 28.6,
                high = 64.3,
                condition = WeatherCondition.Rain,
                precipitationChance = 80,
            ),
        ),
) = Forecast(
    current =
        CurrentConditions(
            observedAt = LocalDateTime(2026, 11, 18, 9, 0),
            temperature = temperature,
            feelsLike = 31.3,
            windSpeed = 4.4,
            condition = WeatherCondition.Clear,
            isDay = true,
        ),
    days = days,
)

/**
 * A repository that answers from memory and records what it was asked.
 *
 * It answers at once with [placesToReturn] and [forecastToReturn]. Set either
 * to null and each request waits in [searches] or [forecasts] for the test to
 * complete it by hand.
 */
class FakeWeatherRepository : WeatherRepository {
    var placesToReturn: List<Place>? = emptyList()
    var forecastToReturn: Forecast? = sampleForecast()

    /** When set, every request fails with it. */
    var failure: WeatherException? = null

    val searchQueries = mutableListOf<String>()
    val forecastRequests = mutableListOf<ForecastRequest>()
    val searches = mutableListOf<CompletableDeferred<List<Place>>>()
    val forecasts = mutableListOf<CompletableDeferred<Forecast>>()

    override suspend fun searchPlaces(query: String): List<Place> {
        searchQueries += query
        failure?.let { throw it }
        return placesToReturn ?: CompletableDeferred<List<Place>>().also { searches += it }.await()
    }

    override suspend fun forecast(
        latitude: Double,
        longitude: Double,
        units: Units,
    ): Forecast {
        forecastRequests += ForecastRequest(latitude, longitude, units)
        failure?.let { throw it }
        return forecastToReturn ?: CompletableDeferred<Forecast>().also { forecasts += it }.await()
    }
}

data class ForecastRequest(
    val latitude: Double,
    val longitude: Double,
    val units: Units,
)

class FakeSettingsRepository(
    override var units: Units? = null,
) : SettingsRepository {
    /** When true, saving fails as a full or forbidden store would. */
    var refusesToSave = false

    override fun saveUnits(units: Units) {
        if (refusesToSave) throw SettingsException()
        this.units = units
    }
}
