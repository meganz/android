package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * English name for primary folder for camera uploads
 */
const val CAMERA_UPLOADS_ENGLISH = "Camera Uploads"

/**
 * Rename primary folder in other language if current device language is not English
 */
class DefaultRenamePrimaryFolder @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val getCameraUploadFolderName: GetCameraUploadFolderName,
) : RenamePrimaryFolder {
    override suspend fun invoke(primaryHandle: Long) {
        val primaryFolderName = getCameraUploadFolderName(false)
        if (primaryFolderName != CAMERA_UPLOADS_ENGLISH) {
            cameraUploadRepository.renameNode(primaryHandle, primaryFolderName)
        }
    }
}
