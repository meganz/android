package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.camerauploads.GetUploadVideoQuality
import javax.inject.Inject

/**
 * Default implementation of [ShouldCompressVideo]
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property getUploadVideoQuality [GetUploadVideoQuality]
 */
class DefaultShouldCompressVideo @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val getUploadVideoQuality: GetUploadVideoQuality,
) : ShouldCompressVideo {

    override suspend fun invoke(): Boolean {
        val qualitySetting = getUploadVideoQuality()
        return qualitySetting != null && qualitySetting != VideoQuality.ORIGINAL
    }
}
