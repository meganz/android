package mega.privacy.android.domain.usecase

/**
 * Reset Camera Upload Timestamps
 */
interface ResetCameraUploadTimeStamps {

    /**
     * Invoke.
     *
     * @param clearCamSyncRecords
     */
    suspend operator fun invoke(clearCamSyncRecords: Boolean)
}