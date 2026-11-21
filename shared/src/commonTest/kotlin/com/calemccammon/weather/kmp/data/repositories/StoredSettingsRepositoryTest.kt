package com.calemccammon.weather.kmp.data.repositories

import com.calemccammon.weather.kmp.domain.models.Units
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StoredSettingsRepositoryTest {
    private val store = MapSettings()

    @Test
    fun `there are no units until some are chosen`() {
        assertNull(StoredSettingsRepository(store).units)
    }

    @Test
    fun `chosen units are there for the next launch`() {
        StoredSettingsRepository(store).saveUnits(Units.Imperial)

        assertEquals(Units.Imperial, StoredSettingsRepository(store).units)

        StoredSettingsRepository(store).saveUnits(Units.Metric)

        assertEquals(Units.Metric, StoredSettingsRepository(store).units)
    }

    @Test
    fun `a stored word this version does not know counts as no choice`() {
        store.putString("units", "kelvin")

        assertNull(StoredSettingsRepository(store).units)
    }
}
