package mega.privacy.android.domain.repository

/**
 * Repository for reverse-geocoding coordinates into human-readable place names.
 */
interface GeocoderRepository {

    /**
     * Reverse-geocode a coordinate into a comma-separated address line
     * (e.g. "Utrecht, Netherlands").
     *
     * @param latitude the GPS latitude
     * @param longitude the GPS longitude
     * @return the address line, or null when it cannot be resolved
     */
    suspend fun getAddressLine(latitude: Double, longitude: Double): String?
}
