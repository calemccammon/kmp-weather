package com.calemccammon.weather.kmp.domain.usecases

import com.calemccammon.weather.kmp.domain.models.Place
import com.calemccammon.weather.kmp.domain.repositories.WeatherRepository

/** Finds places by name. */
class SearchPlaces(
    private val repository: WeatherRepository,
) {
    /**
     * Places matching [query], best match first. Empty, without asking the
     * repository, when the query is too short.
     */
    suspend operator fun invoke(query: String): List<Place> {
        if (!isSearchable(query)) return emptyList()
        return repository.searchPlaces(query.trim())
    }

    companion object {
        /** A query shorter than this is not worth sending: one letter matches nothing useful. */
        const val MIN_QUERY_LENGTH = 2

        /** Whether [query] is long enough to search for, spaces around it aside. */
        fun isSearchable(query: String): Boolean = query.trim().length >= MIN_QUERY_LENGTH
    }
}
