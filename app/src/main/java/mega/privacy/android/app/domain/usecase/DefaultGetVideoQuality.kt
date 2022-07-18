package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Get video quality setting
 *
 */
class DefaultGetVideoQuality @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : GetVideoQuality {

    override fun invoke() = cameraUploadRepository.getVideoQuality().toInt()
}
