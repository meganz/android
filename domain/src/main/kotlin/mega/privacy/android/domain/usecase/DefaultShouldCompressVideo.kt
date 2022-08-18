package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Should compress video
 *
 */
class DefaultShouldCompressVideo @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : ShouldCompressVideo {

    override suspend fun invoke(): Boolean {
        val qualitySetting = cameraUploadRepository.getUploadVideoQuality()
        return qualitySetting != null && qualitySetting.toInt() != VideoQuality.ORIGINAL.value
    }
}
