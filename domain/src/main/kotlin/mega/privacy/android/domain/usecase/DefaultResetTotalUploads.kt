package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Default implementation of [ResetTotalUploads]
 *
 * @property hasPendingUploads [HasPendingUploads]
 * @property cameraUploadRepository [CameraUploadRepository]
 */
class DefaultResetTotalUploads @Inject constructor(
    private val hasPendingUploads: HasPendingUploads,
    private val cameraUploadRepository: CameraUploadRepository,
) : ResetTotalUploads {
    override suspend fun invoke() {
        if (!hasPendingUploads()) {
            cameraUploadRepository.resetTotalUploads()
        }
    }
}