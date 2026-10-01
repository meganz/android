package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use case to clear all Camera Uploads in progress transfers once the Camera Uploads process ends
 */
class ClearCameraUploadsInProgressTransfersUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke() = cameraUploadsRepository.clearCameraUploadsInProgressTransfers()
}
