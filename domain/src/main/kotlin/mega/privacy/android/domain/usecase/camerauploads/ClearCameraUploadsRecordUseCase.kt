package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Clear the camera uploads record
 *
 * @param cameraUploadRepository
 */
class ClearCameraUploadsRecordUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Clear the camera uploads record given the folder types
     *
     * @param folderTypes a list of folder type (Primary, Secondary, or both)
     */
    suspend operator fun invoke(folderTypes: List<CameraUploadFolderType>) =
        cameraUploadRepository.clearRecords(folderTypes)
}
