package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Default implementation of [CompressVideos]
 */
class DefaultCompressVideos @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : CompressVideos {
    override suspend fun invoke(
        rootPath: String,
        pendingList: List<SyncRecord>,
    ) = cameraUploadRepository.compressVideos(
        rootPath,
        cameraUploadRepository.getUploadVideoQuality() ?: VideoQuality.ORIGINAL,
        pendingList
    )
}
