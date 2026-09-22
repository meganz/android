package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use Case that checks whether the new Cloud Drive Folder Node selected by Camera Uploads is valid
 * or not
 *
 * @property cameraUploadsRepository Repository containing all Camera Uploads related operations
 */
class IsNewFolderNodeValidUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Invocation function
     *
     * @param nodeHandle The Node Handle of newly selected Cloud Drive Folder Node, which can be null
     * @return true if the Node Handle is valid
     */
    operator fun invoke(nodeHandle: Long?) =
        nodeHandle != null && nodeHandle != cameraUploadsRepository.getInvalidHandle()
}