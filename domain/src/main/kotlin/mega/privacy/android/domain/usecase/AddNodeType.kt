package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.TypedNode

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
    suspend operator fun invoke(node: Node): TypedNode
}
