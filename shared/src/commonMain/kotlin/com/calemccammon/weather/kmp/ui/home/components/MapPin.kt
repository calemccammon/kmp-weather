package com.calemccammon.weather.kmp.ui.home.components

import androidx.compose.runtime.Immutable

/** Where the pin goes on the map. */
@Immutable
data class MapPin(
    val latitude: Double,
    val longitude: Double,
)
