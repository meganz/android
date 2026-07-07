package mega.privacy.android.feature.fileinfo.presentation.model

/**
 * A valid GPS coordinate pair for a geo-tagged node. Instances only exist for valid locations —
 * build them through [createOrNull] rather than the constructor.
 *
 * @property latitude the GPS latitude
 * @property longitude the GPS longitude
 */
internal data class Coordinates(
    val latitude: Double,
    val longitude: Double,
) {
    companion object {
        private val LATITUDE_RANGE = -90.0..90.0
        private val LONGITUDE_RANGE = -180.0..180.0

        /**
         * Builds [Coordinates] only for a present, in-range location. Returns null for the (0,0)
         * "no location" sentinel (either value being zero) or out-of-range input.
         */
        fun createOrNull(latitude: Double, longitude: Double): Coordinates? {
            val isValid = latitude != 0.0 && longitude != 0.0 &&
                    latitude in LATITUDE_RANGE && longitude in LONGITUDE_RANGE
            return if (isValid) Coordinates(latitude, longitude) else null
        }
    }
}
