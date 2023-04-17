package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case that checks whether the newly selected Primary Folder path from the File Explorer is
 * valid or not
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 */
class IsNewPrimaryFolderPathValidUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invocation function
     *
     * @param newPath The new Primary Folder path
     * @return true if the new Primary Folder exists and is different from the Secondary Folder
     * local path. Otherwise, return false
     */
    suspend operator fun invoke(newPath: String?) = newPath?.let { nonNullPath ->
        val secondaryFolderLocalPath = cameraUploadRepository.getSecondaryFolderLocalPath()
        nonNullPath.isNotBlank() && nonNullPath != secondaryFolderLocalPath
    } ?: false
}