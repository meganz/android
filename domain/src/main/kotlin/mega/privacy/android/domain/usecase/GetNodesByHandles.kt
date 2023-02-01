package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode

/**
 * The use case for getting nodes by handles
 */
interface GetNodesByHandles {

    /**
     * Get nodes by handles
     *
     * @param handles handle list
     * @return nodes
     */
    suspend operator fun invoke(handles: List<Long>): List<TypedNode>
}