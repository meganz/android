package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Monitor Camera Uploads Folder Destination Update Use Case
 */
class MonitorCameraUploadsFolderDestinationUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {
    /**
     * Invoke
     */
    operator fun invoke() = cameraUploadRepository.monitorCameraUploadsFolderDestination()
}
