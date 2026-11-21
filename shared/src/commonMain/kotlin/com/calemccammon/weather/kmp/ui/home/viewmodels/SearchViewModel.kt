package com.calemccammon.weather.kmp.ui.home.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calemccammon.weather.kmp.domain.models.Place
import com.calemccammon.weather.kmp.domain.repositories.WeatherException
import com.calemccammon.weather.kmp.domain.usecases.SearchPlaces
import com.calemccammon.weather.kmp.ui.core.message
import com.calemccammon.weather.kmp.ui.home.models.PlaceUiModel
import com.calemccammon.weather.kmp.ui.home.models.SearchState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Turns what the user types into search results.
 *
 * It waits for a pause in typing before searching, and a search is given up
 * when a newer one replaces it, so an old answer never shows over a new one.
 */
class SearchViewModel(
    private val searchPlaces: SearchPlaces,
) : ViewModel() {
    /** The text last asked about; what [retry] asks about again. */
    private var latestQuery = ""

    /** The places behind the rows of the current [SearchState.Results], in order. */
    private var results: List<Place> = emptyList()

    /** The wait for a pause in typing, and the search after it. */
    private var searching: Job? = null

    private val mutableState = MutableStateFlow<SearchState>(SearchState.Idle)
    val state: StateFlow<SearchState> = mutableState.asStateFlow()

    fun queryChanged(text: String) {
        val query = text.trim()
        if (query == latestQuery) return
        latestQuery = query
        searching?.cancel()

        if (!SearchPlaces.isSearchable(query)) {
            mutableState.value = SearchState.Idle
            return
        }
        mutableState.value = SearchState.Loading(query)
        searching =
            viewModelScope.launch {
                delay(debounce)
                search(query)
            }
    }

    /** Forgets the current query and results, as when the search bar opens. */
    fun reset() {
        searching?.cancel()
        latestQuery = ""
        results = emptyList()
        mutableState.value = SearchState.Idle
    }

    /**
     * The place shown in row [index] of the results, or null if there is no
     * such row. The screen hands it on to be selected.
     */
    fun resultAt(index: Int): Place? = if (state.value is SearchState.Results) results.getOrNull(index) else null

    /** Runs the last search again after a failure. */
    fun retry() {
        val query = latestQuery
        if (!SearchPlaces.isSearchable(query)) return
        searching?.cancel()
        mutableState.value = SearchState.Loading(query)
        searching = viewModelScope.launch { search(query) }
    }

    private suspend fun search(query: String) {
        mutableState.value =
            try {
                val places = searchPlaces(query)
                results = places
                if (places.isEmpty()) {
                    SearchState.Empty(query)
                } else {
                    SearchState.Results(query, places.map(PlaceUiModel::from))
                }
            } catch (e: WeatherException) {
                SearchState.Failed(query, e.failure.message)
            }
    }

    companion object {
        /** How long typing must pause before a search is sent. */
        val debounce = 350.milliseconds
    }
}
