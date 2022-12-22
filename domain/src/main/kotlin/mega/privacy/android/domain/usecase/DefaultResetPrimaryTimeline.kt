package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Reset time stamps for primary media
 */
class DefaultResetPrimaryTimeline @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : ResetPrimaryTimeline {

    override suspend fun invoke() {
        cameraUploadRepository.setSyncTimeStamp(0, SyncTimeStamp.PRIMARY_PHOTO)
        cameraUploadRepository.setSyncTimeStamp(0, SyncTimeStamp.PRIMARY_VIDEO)
        cameraUploadRepository.deleteAllPrimarySyncRecords()
    }
}
