package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.usecase.camerauploads.DisableCameraUploadsSettingsUseCase
import javax.inject.Inject

/**
 * Default Use Case implementation of [DisableCameraUploadsInDatabase]
 */
class DefaultDisableCameraUploadsInDatabase @Inject constructor(
    private val clearSyncRecords: ClearSyncRecords,
    private val disableCameraUploadsSettingsUseCase: DisableCameraUploadsSettingsUseCase,
    private val resetCameraUploadTimeStamps: ResetCameraUploadTimeStamps,
) : DisableCameraUploadsInDatabase {

    override suspend fun invoke() {
        resetCameraUploadTimeStamps(clearCamSyncRecords = true)
        clearSyncRecords()
        disableCameraUploadsSettingsUseCase()
    }
}
