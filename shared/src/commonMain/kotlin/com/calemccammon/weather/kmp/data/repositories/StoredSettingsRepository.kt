package com.calemccammon.weather.kmp.data.repositories

import com.calemccammon.weather.kmp.domain.models.Units
import com.calemccammon.weather.kmp.domain.repositories.SettingsException
import com.calemccammon.weather.kmp.domain.repositories.SettingsRepository
import com.russhwolf.settings.Settings

/** Keeps settings in the platform's own small key-value store. */
class StoredSettingsRepository(
    private val settings: Settings,
) : SettingsRepository {
    override val units: Units?
        get() =
            when (settings.getStringOrNull(UNITS_KEY)) {
                METRIC -> Units.Metric
                IMPERIAL -> Units.Imperial
                // Never chosen, or written by a version with a choice this one lacks.
                else -> null
            }

    // Each platform's store fails in its own way: a browser with storage
    // turned off throws an error of its own, for one.
    @Suppress("TooGenericExceptionCaught")
    override fun saveUnits(units: Units) {
        try {
            settings.putString(
                UNITS_KEY,
                when (units) {
                    Units.Metric -> METRIC
                    Units.Imperial -> IMPERIAL
                },
            )
        } catch (e: Throwable) {
            throw SettingsException(e)
        }
    }

    private companion object {
        const val UNITS_KEY = "units"

        // The stored words are this file's own, so renaming the enum's values
        // cannot orphan a saved choice.
        const val METRIC = "metric"
        const val IMPERIAL = "imperial"
    }
}
