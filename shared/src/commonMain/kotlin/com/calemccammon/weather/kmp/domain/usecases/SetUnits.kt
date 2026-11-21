package com.calemccammon.weather.kmp.domain.usecases

import com.calemccammon.weather.kmp.domain.models.Units
import com.calemccammon.weather.kmp.domain.repositories.SettingsRepository

/** Records the units the user chose, for this launch and later ones. */
class SetUnits(
    private val repository: SettingsRepository,
) {
    operator fun invoke(units: Units) = repository.saveUnits(units)
}
