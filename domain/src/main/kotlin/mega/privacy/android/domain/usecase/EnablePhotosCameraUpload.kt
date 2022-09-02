package mega.privacy.android.domain.usecase

/**
 * The use case interface to enable camera uploads
 */
interface EnablePhotosCameraUpload {
    /**
     * Enable Camera Uploads
     *
     * @param path
     * @param syncVideo whether or not videos will be uploaded
     * @param enableCellularSync whether or not cellular connection will be used
     */
    suspend operator fun invoke(
        path: String?,
        syncVideo: Boolean,
        enableCellularSync: Boolean,
        videoQuality: Int,
        conversionChargingOnSize: Int,
    )
}