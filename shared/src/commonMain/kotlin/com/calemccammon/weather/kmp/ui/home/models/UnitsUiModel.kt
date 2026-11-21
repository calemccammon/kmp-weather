package com.calemccammon.weather.kmp.ui.home.models

import com.calemccammon.weather.kmp.domain.models.Units

/** The choices the units control offers. */
enum class UnitsUiModel {
    Celsius,
    Fahrenheit,
    ;

    fun toDomain(): Units =
        when (this) {
            Celsius -> Units.Metric
            Fahrenheit -> Units.Imperial
        }

    companion object {
        fun from(units: Units): UnitsUiModel =
            when (units) {
                Units.Metric -> Celsius
                Units.Imperial -> Fahrenheit
            }
    }
}
