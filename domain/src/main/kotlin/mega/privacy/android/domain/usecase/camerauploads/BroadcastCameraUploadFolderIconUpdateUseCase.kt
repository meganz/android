package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.CameraUploadFolderIconUpdate
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Broadcast Camera Uploads Folder Icon Update Use Case
 */
class BroadcastCameraUploadFolderIconUpdateUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(data: CameraUploadFolderIconUpdate) =
        cameraUploadRepository.broadcastCameraUploadFolderIconUpdate(data)
}
