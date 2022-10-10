package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Default Implementation of [ResetMediaUploadTimeStamps]
 */
class DefaultResetMediaUploadTimeStamps @Inject constructor(private val cameraUploadRepository: CameraUploadRepository) :
    ResetMediaUploadTimeStamps {
    override suspend fun invoke() {
        cameraUploadRepository.run {
            setSyncTimeStamp(0, SyncTimeStamp.SECONDARY_PHOTO)
            setSyncTimeStamp(0, SyncTimeStamp.SECONDARY_VIDEO)
        }
    }
}