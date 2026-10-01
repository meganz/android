package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.GeocoderRepository
import javax.inject.Inject

/**
 * Reverse-geocode a coordinate into a human-readable address line.
 */
class GetAddressFromCoordinatesUseCase @Inject constructor(
    private val geocoderRepository: GeocoderRepository,
) {
    /**
     * @param latitude the GPS latitude
     * @param longitude the GPS longitude
     * @return a comma-separated address line (e.g. "Utrecht, Netherlands"), or null when unavailable
     */
    suspend operator fun invoke(latitude: Double, longitude: Double): String? =
        geocoderRepository.getAddressLine(latitude, longitude)
}
