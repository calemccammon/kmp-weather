package com.calemccammon.weather.kmp.domain.repositories

import com.calemccammon.weather.kmp.domain.models.Units

/** What the user has chosen for the app, kept between launches. */
interface SettingsRepository {
    /** The units the user chose, or null if they have never chosen. */
    val units: Units?

    /** Fails with a [SettingsException] if the choice could not be kept. */
    fun saveUnits(units: Units)
}

/** Thrown when the device would not store a setting. */
class SettingsException(
    cause: Throwable? = null,
) : Exception("The setting was not saved", cause)
