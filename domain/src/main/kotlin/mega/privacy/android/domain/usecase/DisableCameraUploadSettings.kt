package mega.privacy.android.domain.usecase

/**
 * Disable Camera Upload Setting
 *
 */
fun interface DisableCameraUploadSettings {

    /**
     * Invoke
     *
     * @param clearCamSyncRecords
     */
    suspend operator fun invoke(clearCamSyncRecords: Boolean)
}