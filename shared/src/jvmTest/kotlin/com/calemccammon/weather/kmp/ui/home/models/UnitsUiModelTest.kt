package com.calemccammon.weather.kmp.ui.home.models

import com.calemccammon.weather.kmp.domain.models.Units
import kotlin.test.Test
import kotlin.test.assertEquals

class UnitsUiModelTest {
    @Test
    fun `each choice stands for one system of units and back`() {
        assertEquals(UnitsUiModel.Celsius, UnitsUiModel.from(Units.Metric))
        assertEquals(UnitsUiModel.Fahrenheit, UnitsUiModel.from(Units.Imperial))
        for (units in Units.entries) assertEquals(units, UnitsUiModel.from(units).toDomain())
    }
}
