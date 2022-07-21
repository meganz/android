package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
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
