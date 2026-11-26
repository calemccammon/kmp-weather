package com.calemccammon.weather.kmp.ui.home.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calemccammon.weather.kmp.domain.models.Forecast
import com.calemccammon.weather.kmp.domain.models.Place
import com.calemccammon.weather.kmp.domain.models.Units
import com.calemccammon.weather.kmp.domain.repositories.SettingsException
import com.calemccammon.weather.kmp.domain.repositories.WeatherException
import com.calemccammon.weather.kmp.domain.usecases.GetForecast
import com.calemccammon.weather.kmp.domain.usecases.GetUnits
import com.calemccammon.weather.kmp.domain.usecases.SetUnits
import com.calemccammon.weather.kmp.ui.core.message
import com.calemccammon.weather.kmp.ui.home.models.ForecastState
import com.calemccammon.weather.kmp.ui.home.models.ForecastUiModel
import com.calemccammon.weather.kmp.ui.home.models.HomeState
import com.calemccammon.weather.kmp.ui.home.models.PlaceUiModel
import com.calemccammon.weather.kmp.ui.home.models.UnitsUiModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds the selected place and the chosen units, and loads the forecast.
 *
 * The forecast is held as it came, in metric units, and converted for the
 * screen, so choosing other units never asks for it again.
 *
 * Selecting a place while an earlier forecast is still on its way gives the
 * earlier request up, so its answer never replaces the newer one.
 */
class HomeViewModel(
    private val getForecast: GetForecast,
    getUnits: GetUnits,
    private val saveUnits: SetUnits,
    /** The country the device is set to, such as "US", if it says. */
    deviceCountry: String?,
) : ViewModel() {
    private var place: Place? = null
    private var units: Units = getUnits(deviceCountry)

    /** The selected place's forecast in metric units, once it has arrived. */
    private var forecast: Forecast? = null

    /** The forecast request under way, if any. */
    private var loading: Job? = null

    private val mutableState = MutableStateFlow(HomeState(units = UnitsUiModel.from(units)))
    val state: StateFlow<HomeState> = mutableState.asStateFlow()

    /** Makes [place] the selected place and loads its forecast. */
    fun select(place: Place) {
        this.place = place
        load()
    }

    /** Selects a point picked straight off the map. */
    fun selectPoint(
        latitude: Double,
        longitude: Double,
    ) = select(Place.fromCoordinates(latitude, longitude))

    fun clear() {
        loading?.cancel()
        place = null
        forecast = null
        mutableState.value = HomeState(units = UnitsUiModel.from(units))
    }

    /**
     * Shows forecasts in [chosen] from now on, and remembers the choice. The
     * forecast on screen is converted at once, without asking for it again; one
     * still on its way arrives in them.
     */
    fun setUnits(chosen: UnitsUiModel) {
        val units = chosen.toDomain()
        if (units == this.units) return
        this.units = units
        try {
            saveUnits(units)
        } catch (_: SettingsException) {
            // Not saved, so the next launch starts from the old choice; this
            // one carries on with the new.
        }
        // Loading or failed, there is nothing to convert yet.
        val converted = forecast?.let { ForecastState.Loaded(ForecastUiModel.from(it, units)) }
        mutableState.update { it.copy(units = chosen, forecast = converted ?: it.forecast) }
    }

    /** Asks for the selected place's forecast again, after a failure. */
    fun retryForecast() = load()

    private fun load() {
        val place = place ?: return
        loading?.cancel()
        forecast = null
        mutableState.value =
            HomeState(
                units = UnitsUiModel.from(units),
                place = PlaceUiModel.from(place),
                forecast = ForecastState.Loading,
            )
        loading =
            viewModelScope.launch {
                val outcome =
                    try {
                        val forecast = getForecast(place)
                        this@HomeViewModel.forecast = forecast
                        // In the units chosen by the time it arrives.
                        ForecastState.Loaded(ForecastUiModel.from(forecast, units))
                    } catch (e: WeatherException) {
                        ForecastState.Failed(e.failure.message)
                    }
                mutableState.update { it.copy(forecast = outcome) }
            }
    }
}
