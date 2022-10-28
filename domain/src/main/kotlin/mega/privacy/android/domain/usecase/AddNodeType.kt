package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode

/**
 * Add node type
 */
fun interface AddNodeType {
    /**
     * Invoke
     *
     * @param node
     * @return typed node
     */
    suspend operator fun invoke(node: UnTypedNode): TypedNode
}
