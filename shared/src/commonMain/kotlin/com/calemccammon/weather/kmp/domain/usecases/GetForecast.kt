package com.calemccammon.weather.kmp.domain.usecases

import com.calemccammon.weather.kmp.domain.models.Forecast
import com.calemccammon.weather.kmp.domain.models.Place
import com.calemccammon.weather.kmp.domain.repositories.WeatherException
import com.calemccammon.weather.kmp.domain.repositories.WeatherFailure
import com.calemccammon.weather.kmp.domain.repositories.WeatherRepository
import kotlin.math.abs

/** Gets the forecast for a place. */
class GetForecast(
    private val repository: WeatherRepository,
) {
    /**
     * The forecast at [place], in metric units. [Forecast.inUnits] gives it
     * in others.
     *
     * The longitude is brought into range first (see [Place.normalizeLongitude]).
     * A point off the globe fails with [WeatherFailure.InvalidPlace] without
     * asking the repository.
     */
    suspend operator fun invoke(place: Place): Forecast {
        // Written so that a NaN fails the check too.
        val onGlobe = abs(place.latitude) <= MAX_LATITUDE && place.longitude.isFinite()
        if (!onGlobe) throw WeatherException(WeatherFailure.InvalidPlace)

        return repository.forecast(
            latitude = place.latitude,
            longitude = Place.normalizeLongitude(place.longitude),
        )
    }

    private companion object {
        const val MAX_LATITUDE = 90.0
    }
}
