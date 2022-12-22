package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Get is charging required
 *
 */
class DefaultIsChargingRequired @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : IsChargingRequired {

    override suspend fun invoke(queueSize: Long) =
        cameraUploadRepository.convertOnCharging() && queueSize > cameraUploadRepository.getChargingOnSize()
}
