package mega.privacy.android.domain.usecase

import javax.inject.Inject

/**
 * Default Use Case implementation of [DisableCameraUploadsInDatabase]
 */
class DefaultDisableCameraUploadsInDatabase @Inject constructor(
    private val clearSyncRecords: ClearSyncRecords,
    private val disableCameraUploadSettings: DisableCameraUploadSettings,
    private val resetCameraUploadTimeStamps: ResetCameraUploadTimeStamps,
) : DisableCameraUploadsInDatabase {

    override suspend fun invoke() {
        resetCameraUploadTimeStamps(clearCamSyncRecords = true)
        clearSyncRecords()
        disableCameraUploadSettings()
    }
}