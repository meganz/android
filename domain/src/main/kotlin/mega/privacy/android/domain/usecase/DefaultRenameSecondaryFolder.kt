package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * English name for secondary folder for camera uploads
 */
const val SECONDARY_UPLOADS_ENGLISH = "Media Uploads"

/**
 * Rename secondary folder in other language if current device language is not English
 */
class DefaultRenameSecondaryFolder @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val getCameraUploadFolderName: GetCameraUploadFolderName,
) : RenameSecondaryFolder {
    override suspend fun invoke(secondaryHandle: Long) {
        val secondaryFolderName = getCameraUploadFolderName(true)
        if (secondaryFolderName != SECONDARY_UPLOADS_ENGLISH) {
            cameraUploadRepository.renameNode(secondaryHandle, secondaryFolderName)
        }
    }
}
