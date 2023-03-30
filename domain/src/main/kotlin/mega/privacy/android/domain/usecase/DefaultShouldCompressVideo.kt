package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.camerauploads.GetUploadVideoQualityUseCase
import javax.inject.Inject

/**
 * Default implementation of [ShouldCompressVideo]
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property getUploadVideoQuality [GetUploadVideoQualityUseCase]
 */
class DefaultShouldCompressVideo @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val getUploadVideoQuality: GetUploadVideoQualityUseCase,
) : ShouldCompressVideo {

    override suspend fun invoke(): Boolean {
        val qualitySetting = getUploadVideoQuality()
        return qualitySetting != null && qualitySetting != VideoQuality.ORIGINAL
    }
}
