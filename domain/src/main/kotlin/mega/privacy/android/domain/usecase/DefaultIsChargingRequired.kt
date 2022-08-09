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

    override fun invoke(queueSize: Long): Boolean {

        if (cameraUploadRepository.convertOnCharging()) {
            val queueSizeLimit = cameraUploadRepository.getChargingOnSize()
            if (queueSize > queueSizeLimit) {
                return true
            }
        }
        return false
    }
}
