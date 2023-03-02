package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.camerauploads.GetUploadVideoQuality
import javax.inject.Inject

/**
 * Default implementation of [CompressVideos]
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property getUploadVideoQuality [GetUploadVideoQuality]
 */
class DefaultCompressVideos @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val getUploadVideoQuality: GetUploadVideoQuality,
) : CompressVideos {
    override suspend fun invoke(
        rootPath: String,
        pendingList: List<SyncRecord>,
    ) = cameraUploadRepository.compressVideos(
        root = rootPath,
        quality = getUploadVideoQuality() ?: VideoQuality.ORIGINAL,
        records = pendingList,
    )
}
