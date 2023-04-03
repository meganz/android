package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.camerauploads.GetUploadVideoQualityUseCase
import javax.inject.Inject

/**
 * Default implementation of [CompressedVideoPending]
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property getUploadVideoQualityUseCase [GetUploadVideoQualityUseCase]
 */
class DefaultCompressedVideoPending @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val getUploadVideoQualityUseCase: GetUploadVideoQualityUseCase,
) : CompressedVideoPending {
    override suspend fun invoke(): Boolean =
        getUploadVideoQualityUseCase() != VideoQuality.ORIGINAL && cameraUploadRepository.getVideoSyncRecordsByStatus(
            SyncStatus.STATUS_TO_COMPRESS
        ).isNotEmpty()
}
