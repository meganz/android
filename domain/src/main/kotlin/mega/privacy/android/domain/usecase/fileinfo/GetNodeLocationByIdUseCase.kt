package mega.privacy.android.domain.usecase.fileinfo

import mega.privacy.android.domain.entity.NodeLocation
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case to determine the node location by id
 */
class GetNodeLocationByIdUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Determine the node location by id
     *
     * @param nodeId The node id to determine location for
     * @return The node location, defaults to CloudDrive if node is not found
     */
    suspend operator fun invoke(nodeId: NodeId): NodeLocation {
        return nodeRepository.getNodeLocationById(nodeId)
            ?: NodeLocation.CloudDrive
    }
}

