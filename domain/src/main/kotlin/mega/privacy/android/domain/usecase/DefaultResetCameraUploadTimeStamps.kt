package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Default Implementation of [ResetCameraUploadTimeStamps]
 */
class DefaultResetCameraUploadTimeStamps @Inject constructor(private val cameraUploadRepository: CameraUploadRepository) :
    ResetCameraUploadTimeStamps {
    override suspend fun invoke(clearCamSyncRecords: Boolean) {
        cameraUploadRepository.run {
            setSyncTimeStamp(0, SyncTimeStamp.PRIMARY_PHOTO)
            setSyncTimeStamp(0, SyncTimeStamp.PRIMARY_VIDEO)
            setSyncTimeStamp(0, SyncTimeStamp.SECONDARY_PHOTO)
            setSyncTimeStamp(0, SyncTimeStamp.SECONDARY_VIDEO)
            saveShouldClearCamSyncRecords(clearCamSyncRecords)
        }
    }
}