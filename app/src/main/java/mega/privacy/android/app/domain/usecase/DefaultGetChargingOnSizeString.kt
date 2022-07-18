package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.CameraUploadRepository
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
