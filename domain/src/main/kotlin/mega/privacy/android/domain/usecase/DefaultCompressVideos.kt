package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.camerauploads.GetUploadVideoQualityUseCase
import javax.inject.Inject

/**
 * Default implementation of [CompressVideos]
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property getUploadVideoQualityUseCase [GetUploadVideoQualityUseCase]
 */
class DefaultCompressVideos @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val getUploadVideoQualityUseCase: GetUploadVideoQualityUseCase,
) : CompressVideos {
    override fun invoke(
        rootPath: String,
        pendingList: List<SyncRecord>,
    ) = flow {
        emitAll(
            cameraUploadRepository.compressVideos(
                root = rootPath,
                quality = getUploadVideoQualityUseCase() ?: VideoQuality.ORIGINAL,
                records = pendingList,
            )
        )
    }
}
