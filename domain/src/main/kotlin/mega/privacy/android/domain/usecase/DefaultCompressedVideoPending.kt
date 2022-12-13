package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * If compressed video is pending
 *
 */
class DefaultCompressedVideoPending @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : CompressedVideoPending {

    override suspend fun invoke(): Boolean {
        return cameraUploadRepository.getUploadVideoQuality() != VideoQuality.ORIGINAL && cameraUploadRepository.getVideoSyncRecordsByStatus(
            SyncStatus.STATUS_TO_COMPRESS)
            .isNotEmpty()
    }
}
