package mega.privacy.android.domain.usecase

/**
 * Disable Camera Upload Setting Process
 *
 */
fun interface DisableCameraUploadSettingProcess {

    /**
     * Invoke
     *
     * @param clearCamSyncRecords
     */
    suspend operator fun invoke(clearCamSyncRecords: Boolean)
}