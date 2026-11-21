package com.calemccammon.weather.kmp.domain.models

/** The two systems of units the app can show a forecast in. */
enum class Units {
    Metric,
    Imperial,
    ;

    companion object {
        /** The United States, Liberia and Myanmar use Fahrenheit and miles. */
        private val imperialCountries = setOf("US", "LR", "MM")

        /** The units of the country with this code, such as "US". */
        fun forCountry(countryCode: String?): Units = if (countryCode in imperialCountries) Imperial else Metric
    }
}
