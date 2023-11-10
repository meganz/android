package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case that checks whether compressing videos require the device to be charged or not
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 */
class IsChargingRequiredForVideoCompressionUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invocation function
     *
     * @return true if the device needs to be charged to compress videos, and false if otherwise
     */
    suspend operator fun invoke(): Boolean =
        cameraUploadRepository.isChargingRequiredForVideoCompression() ?: true
}
