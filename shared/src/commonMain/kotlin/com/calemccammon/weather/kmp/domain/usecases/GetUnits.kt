package com.calemccammon.weather.kmp.domain.usecases

import com.calemccammon.weather.kmp.domain.models.Units
import com.calemccammon.weather.kmp.domain.repositories.SettingsRepository

/** Decides which units to show a forecast in. */
class GetUnits(
    private val repository: SettingsRepository,
) {
    /**
     * The user's own choice if they have made one; otherwise the units of the
     * country the device is set to ([deviceCountry], such as "US").
     */
    operator fun invoke(deviceCountry: String?): Units = repository.units ?: Units.forCountry(deviceCountry)
}
