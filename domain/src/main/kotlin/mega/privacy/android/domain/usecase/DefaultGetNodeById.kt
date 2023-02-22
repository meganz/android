package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * The implementation of [GetNodeById]
 */
class DefaultGetNodeById @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val addNodeType: AddNodeType,
) : GetNodeById {
    override suspend fun invoke(id: NodeId): TypedNode =
        addNodeType(nodeRepository.getNodeById(id) as UnTypedNode)
}