package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Default Implementation of [ResetCameraUploadTimeStamps]
 */
class DefaultResetCameraUploadTimeStamps @Inject constructor(private val cameraUploadRepository: CameraUploadRepository) :
    ResetCameraUploadTimeStamps {
    override suspend fun invoke(clearCamSyncRecords: Boolean) {
        cameraUploadRepository.run {
            setCamSyncTimeStamp(0)
            setCamVideoSyncTimeStamp(0)
            setSecSyncTimeStamp(0)
            setSecVideoSyncTimeStamp(0)
            saveShouldClearCamSyncRecords(clearCamSyncRecords)
        }
    }
}