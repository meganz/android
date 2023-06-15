package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Monitor Camera Uploads Folder Icon Update Use Case
 */
class MonitorCameraUploadFolderIconUpdateUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {
    /**
     * Invoke
     */
    operator fun invoke() = cameraUploadRepository.monitorCameraUploadFolderIconUpdate()
}
