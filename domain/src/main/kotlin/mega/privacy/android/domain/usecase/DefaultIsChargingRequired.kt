package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.camerauploads.IsChargingRequiredForVideoCompression
import javax.inject.Inject

/**
 * Default implementation of [IsChargingRequired]
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 * @property isChargingRequiredForVideoCompression [IsChargingRequiredForVideoCompression]
 */
class DefaultIsChargingRequired @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val isChargingRequiredForVideoCompression: IsChargingRequiredForVideoCompression,
) : IsChargingRequired {

    override suspend fun invoke(queueSize: Long) =
        isChargingRequiredForVideoCompression() && queueSize > cameraUploadRepository.getChargingOnSize()
}
