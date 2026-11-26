package com.calemccammon.weather.kmp.domain.repositories

import com.calemccammon.weather.kmp.domain.models.Forecast
import com.calemccammon.weather.kmp.domain.models.Place

/**
 * Where the app gets places and forecasts from.
 *
 * The domain only says what it needs. Which service answers, and what that
 * service's responses look like, is the data layer's business.
 *
 * Both calls fail with a [WeatherException] and nothing else.
 */
interface WeatherRepository {
    /** Places whose name matches [query], best match first. */
    suspend fun searchPlaces(query: String): List<Place>

    /** Current conditions and the days ahead at a point, in metric units. */
    suspend fun forecast(
        latitude: Double,
        longitude: Double,
    ): Forecast
}

/** The ways getting weather can fail, as far as the app cares. */
enum class WeatherFailure {
    /** No answer in time. */
    Timeout,

    /** The service could not be reached at all. */
    NoConnection,

    /** An answer came, but not one the app understands. */
    BadResponse,

    /** The service refused the request. */
    Rejected,

    /** The service is in trouble or too busy; trying later may work. */
    Unavailable,

    /** The place asked about is not a point on the globe. */
    InvalidPlace,
}

/**
 * Thrown when a place search or a forecast cannot be had.
 *
 * It says what kind of failure it was; wording that for the user is the UI's
 * job.
 */
class WeatherException(
    val failure: WeatherFailure,
    /** Anything more the source said, for logs. Not for showing to the user. */
    val detail: String? = null,
    cause: Throwable? = null,
) : Exception(if (detail == null) failure.name else "${failure.name}: $detail", cause)
