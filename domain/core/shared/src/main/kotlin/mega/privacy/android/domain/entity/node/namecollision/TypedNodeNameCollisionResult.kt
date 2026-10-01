package mega.privacy.android.domain.entity.node.namecollision

import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * [TypedNode] name collision result
 *
 * @property noConflictNodes
 * @property conflictNodes
 */
data class TypedNodeNameCollisionResult(
    val noConflictNodes: List<TypedNode>,
    val conflictNodes: List<NodeNameCollision>,
)