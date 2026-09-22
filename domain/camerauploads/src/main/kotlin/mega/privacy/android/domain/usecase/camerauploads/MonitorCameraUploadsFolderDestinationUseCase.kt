package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Monitor Camera Uploads Folder Destination Update Use Case
 */
class MonitorCameraUploadsFolderDestinationUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {
    /**
     * Invoke
     */
    operator fun invoke() = cameraUploadsRepository.monitorCameraUploadsFolderDestination()
}
