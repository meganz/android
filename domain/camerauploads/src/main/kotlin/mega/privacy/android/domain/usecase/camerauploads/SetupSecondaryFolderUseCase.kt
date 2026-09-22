package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Setup Secondary Folder for Camera Upload
 *
 */
class SetupSecondaryFolderUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
    private val setSecondaryNodeIdUseCase: SetSecondaryNodeIdUseCase,
) {

    /**
     * Invoke
     *
     * @param secondaryHandle
     */
    suspend operator fun invoke(secondaryHandle: Long) {
        cameraUploadsRepository.setupSecondaryFolder(secondaryHandle)
            .takeIf { it != cameraUploadsRepository.getInvalidHandle() }
            ?.let {
                setSecondaryNodeIdUseCase(NodeId(it))
            }
    }
}

