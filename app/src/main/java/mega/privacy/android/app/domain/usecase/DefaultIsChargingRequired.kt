package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.CameraUploadRepository
import timber.log.Timber
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
                Timber.d("isChargingRequired %s, queue size is %d, limit size is %d",
                    true,
                    queueSize,
                    queueSizeLimit)
                return true
            }
        }
        Timber.d("isChargingRequired %s", false)
        return false
    }
}
