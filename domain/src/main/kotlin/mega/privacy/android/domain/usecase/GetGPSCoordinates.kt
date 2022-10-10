package mega.privacy.android.domain.usecase

/**
 * Get GPS coordinates from file
 */
interface GetGPSCoordinates {
    /**
     * Invoke
     *
     * @param filePath
     * @param isVideo
     *
     * @return latitude and longitude
     */
    suspend operator fun invoke(filePath: String, isVideo: Boolean): Pair<Float, Float>
}
