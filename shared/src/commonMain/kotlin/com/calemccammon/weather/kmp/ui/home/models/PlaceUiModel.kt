package com.calemccammon.weather.kmp.ui.home.models

import androidx.compose.runtime.Immutable
import com.calemccammon.weather.kmp.domain.models.Place
import com.calemccammon.weather.kmp.ui.core.UiText
import com.calemccammon.weather.kmp.ui.core.uiText
import kmp_weather.shared.generated.resources.Res
import kmp_weather.shared.generated.resources.place_coordinates
import kmp_weather.shared.generated.resources.place_east
import kmp_weather.shared.generated.resources.place_north
import kmp_weather.shared.generated.resources.place_south
import kmp_weather.shared.generated.resources.place_west
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * A place as the screen shows it: in a search result, in the search bar, as
 * the forecast's heading and as the pin on the map.
 */
@Immutable
data class PlaceUiModel(
    /** "Asheville", or "35.60°N, 82.55°W" for a place with no name. */
    val title: UiText,
    /** "North Carolina, United States", or empty when nothing more is known. */
    val subtitle: String,
    /** Where the pin goes. */
    val latitude: Double,
    val longitude: Double,
) {
    companion object {
        fun from(place: Place) =
            PlaceUiModel(
                title = place.name?.let(UiText::Verbatim) ?: coordinates(place.latitude, place.longitude),
                subtitle = listOfNotNull(place.region, place.country).joinToString(", "),
                latitude = place.latitude,
                longitude = place.longitude,
            )

        /** Coordinates are shown to two decimal places of a degree: about a kilometre. */
        private const val HUNDREDTHS = 100

        /** "35.60°N, 82.55°W": what a place with no name is called. */
        private fun coordinates(
            latitude: Double,
            longitude: Double,
        ): UiText {
            val north = (latitude * HUNDREDTHS).roundToLong()
            val east = (longitude * HUNDREDTHS).roundToLong()
            // The side is read off the rounded figure, so a point a hair south
            // of the equator is "0.00°N" like the equator itself.
            return uiText(
                Res.string.place_coordinates,
                uiText(if (north >= 0) Res.string.place_north else Res.string.place_south, degrees(north)),
                uiText(if (east >= 0) Res.string.place_east else Res.string.place_west, degrees(east)),
            )
        }

        /** "35.60" for 3560 hundredths of a degree, either side of zero. */
        private fun degrees(hundredths: Long): String {
            val size = abs(hundredths)
            return "${size / HUNDREDTHS}.${(size % HUNDREDTHS).toString().padStart(2, '0')}"
        }
    }
}
