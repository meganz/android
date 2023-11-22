package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use case to rename a node.
 *
 */
class RenameNodeUseCase @Inject constructor(
    private val defaultCameraUploadsRepository: CameraUploadRepository,
) {

    /**
     * Rename a node by [nodeHandle] to a [newName]
     */
    suspend operator fun invoke(nodeHandle: Long, newName: String) {
        defaultCameraUploadsRepository.renameNode(nodeHandle, newName)
    }
}
