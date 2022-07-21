package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Get charging on size
 *
 */
class DefaultGetChargingOnSizeString @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : GetChargingOnSizeString {

    override fun invoke() = cameraUploadRepository.getChargingOnSizeString()
}
