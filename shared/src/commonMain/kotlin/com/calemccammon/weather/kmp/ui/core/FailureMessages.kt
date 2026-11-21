package com.calemccammon.weather.kmp.ui.core

import com.calemccammon.weather.kmp.domain.repositories.WeatherFailure
import kmp_weather.shared.generated.resources.Res
import kmp_weather.shared.generated.resources.failure_bad_response
import kmp_weather.shared.generated.resources.failure_invalid_place
import kmp_weather.shared.generated.resources.failure_no_connection
import kmp_weather.shared.generated.resources.failure_rejected
import kmp_weather.shared.generated.resources.failure_timeout
import kmp_weather.shared.generated.resources.failure_unavailable

/** What to tell the user about each kind of failure. */
val WeatherFailure.message: UiText
    get() =
        uiText(
            when (this) {
                WeatherFailure.Timeout -> Res.string.failure_timeout
                WeatherFailure.NoConnection -> Res.string.failure_no_connection
                WeatherFailure.BadResponse -> Res.string.failure_bad_response
                WeatherFailure.Rejected -> Res.string.failure_rejected
                WeatherFailure.Unavailable -> Res.string.failure_unavailable
                WeatherFailure.InvalidPlace -> Res.string.failure_invalid_place
            },
        )
