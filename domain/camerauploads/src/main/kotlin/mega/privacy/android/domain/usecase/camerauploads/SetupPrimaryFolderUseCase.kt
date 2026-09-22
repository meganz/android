package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Setup Primary Folder for Camera Upload
 *
 */
class SetupPrimaryFolderUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
    private val setPrimaryNodeIdUseCase: SetPrimaryNodeIdUseCase,
) {

    /**
     * Invoke
     *
     * @param primaryHandle
     */
    suspend operator fun invoke(primaryHandle: Long) {
        cameraUploadsRepository.setupPrimaryFolder(primaryHandle)
            .takeIf { it != cameraUploadsRepository.getInvalidHandle() }
            ?.let {
                setPrimaryNodeIdUseCase(NodeId(it))
            }
    }
}
