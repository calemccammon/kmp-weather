package com.calemccammon.weather.kmp.domain.models

/** A point on the map. */
data class Place(
    val latitude: Double,
    val longitude: Double,
    /** What the place is called. A point picked straight off the map has none. */
    val name: String? = null,
    /** State, province or similar. Tells two places of one name apart. */
    val region: String? = null,
    val country: String? = null,
) {
    companion object {
        private const val HALF_TURN = 180.0
        private const val FULL_TURN = 360.0

        /**
         * A place picked straight off the map, which has no name.
         *
         * A map that has been dragged round the world more than once reports
         * longitudes past 180 degrees, so the longitude is brought back into range.
         */
        fun fromCoordinates(
            latitude: Double,
            rawLongitude: Double,
        ) = Place(latitude = latitude, longitude = normalizeLongitude(rawLongitude))

        /** [longitude] as the same meridian between -180 (inclusive) and 180. */
        fun normalizeLongitude(longitude: Double): Double = (longitude + HALF_TURN).mod(FULL_TURN) - HALF_TURN
    }
}
