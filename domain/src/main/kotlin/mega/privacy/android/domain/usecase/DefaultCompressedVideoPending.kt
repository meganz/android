package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.camerauploads.GetUploadVideoQuality
import javax.inject.Inject

/**
 * Default implementation of [CompressedVideoPending]
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property getUploadVideoQuality [GetUploadVideoQuality]
 */
class DefaultCompressedVideoPending @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val getUploadVideoQuality: GetUploadVideoQuality,
) : CompressedVideoPending {
    override suspend fun invoke(): Boolean =
        getUploadVideoQuality() != VideoQuality.ORIGINAL && cameraUploadRepository.getVideoSyncRecordsByStatus(
            SyncStatus.STATUS_TO_COMPRESS
        ).isNotEmpty()
}
