package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.AddNodeType
import javax.inject.Inject

/**
 * Use case to get a [TypedNode] by its [NodeId]
 * First it attempts to get the node from MegaApi, if fails it tried to get the node from MegaFolderApi
 */
class GetPublicNodeByIdUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val addNodeType: AddNodeType,
) {
    /**
     * Invoke
     *
     * @param id [NodeId] of the node
     * @return  [TypedNode]
     */
    suspend operator fun invoke(id: NodeId): TypedNode? = nodeRepository.getNodeByHandle(
        handle = id.longValue,
        attemptFromFolderApi = true
    )?.let {
        addNodeType(it)
    }
}
