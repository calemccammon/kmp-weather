package com.calemccammon.weather.kmp.ui.home.models

import androidx.compose.runtime.Immutable
import com.calemccammon.weather.kmp.ui.core.UiText

/** What the forecast area is showing. */
@Immutable
sealed interface ForecastState {
    /** No place is selected, so there is nothing to forecast. */
    data object Idle : ForecastState

    data object Loading : ForecastState

    data class Loaded(
        val forecast: ForecastUiModel,
    ) : ForecastState

    data class Failed(
        val message: UiText,
    ) : ForecastState
}

/**
 * What the home screen is showing: the selected place, if any, where its
 * forecast has got to, and the units it is in.
 */
@Immutable
data class HomeState(
    val units: UnitsUiModel,
    val place: PlaceUiModel? = null,
    val forecast: ForecastState = ForecastState.Idle,
)
