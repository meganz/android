package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use case to rename a node.
 *
 */
class RenameNodeUseCase @Inject constructor(
    private val defaultCameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Rename a node by [nodeHandle] to a [newName]
     */
    suspend operator fun invoke(nodeHandle: Long, newName: String) {
        defaultCameraUploadsRepository.renameNode(nodeHandle, newName)
    }
}
