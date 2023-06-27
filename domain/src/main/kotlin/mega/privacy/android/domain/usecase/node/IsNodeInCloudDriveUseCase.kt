package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject


/**
 * Use Case that returns true when the node is in cloud drive.
 */
class IsNodeInCloudDriveUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {

    /**
     * @param handle
     * @return Boolean that determines whether the node is in cloud drive or not
     */
    suspend operator fun invoke(handle: Long) = nodeRepository.isNodeInCloudDrive(handle)
}