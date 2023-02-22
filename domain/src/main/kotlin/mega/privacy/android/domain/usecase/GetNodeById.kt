package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * The use case for getting nodes by handles
 */
interface GetNodeById {

    /**
     * Get nodes by handles
     *
     * @param id [NodeId] of the node
     * @return [TypedNode]
     */
    suspend operator fun invoke(id: NodeId): TypedNode
}