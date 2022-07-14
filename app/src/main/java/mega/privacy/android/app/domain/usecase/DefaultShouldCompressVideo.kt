package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Should compress video
 *
 */
class DefaultShouldCompressVideo @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : ShouldCompressVideo {

    override fun invoke(): Boolean {
        val qualitySetting = cameraUploadRepository.getUploadVideoQuality()
        return qualitySetting != null && qualitySetting.toInt() != SettingsConstants.VIDEO_QUALITY_ORIGINAL
    }
}
