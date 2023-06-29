package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.usecase.ClearSyncRecords
import mega.privacy.android.domain.usecase.ResetCameraUploadTimeStamps
import javax.inject.Inject

/**
 * Disable camera uploads and clean up settings and resources
 */
class DisableCameraUploadsUseCase @Inject constructor(
    private val clearSyncRecords: ClearSyncRecords,
    private val disableCameraUploadsSettingsUseCase: DisableCameraUploadsSettingsUseCase,
    private val resetCameraUploadTimeStamps: ResetCameraUploadTimeStamps,
) {

    /**
     * Disable camera uploads and clean up settings and resources
     */
    suspend operator fun invoke() {
        resetCameraUploadTimeStamps(clearCamSyncRecords = true)
        clearSyncRecords()
        disableCameraUploadsSettingsUseCase()
    }
}
