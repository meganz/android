package mega.privacy.android.domain.entity.node

/**
 * Node name collision result
 *
 * @property noConflictNodes
 * @property conflictNodes
 * @property type
 */
data class NodeNameCollisionResult(
    val noConflictNodes: Map<Long, Long>,
    val conflictNodes: Map<Long, NodeNameCollision>,
    val type: NodeNameCollisionType,
)