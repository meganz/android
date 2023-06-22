package mega.privacy.android.domain.usecase.filenode

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Get the versions of the node including the current one
 */
class GetNodeVersionsByHandleUseCase @Inject constructor(private val nodeRepository: NodeRepository) {
    /**
     * @return a list of the versions of the node referenced by its handle [NodeId] including the current one
     */
    suspend operator fun invoke(nodeId: NodeId) = nodeRepository.getNodeHistoryVersions(nodeId)
}