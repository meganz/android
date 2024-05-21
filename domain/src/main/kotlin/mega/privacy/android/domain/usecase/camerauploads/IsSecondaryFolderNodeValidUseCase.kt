package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use Case that checks whether the Node Handle of the Secondary Folder is valid or not
 *
 * @property cameraUploadsRepository [CameraUploadsRepository]
 * @property getPrimarySyncHandleUseCase [GetPrimarySyncHandleUseCase]
 */
class IsSecondaryFolderNodeValidUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
    private val getPrimarySyncHandleUseCase: GetPrimarySyncHandleUseCase,
) {

    /**
     * Invocation function
     *
     * @param nodeHandle The Node Handle of the Secondary Folder, which can be null
     * @return true if the Secondary Folder Node Handle is valid
     */
    suspend operator fun invoke(nodeHandle: Long?) = nodeHandle?.let { secondaryHandle ->
        secondaryHandle != cameraUploadsRepository.getInvalidHandle() && secondaryHandle != getPrimarySyncHandleUseCase()
    } ?: false
}