package mega.privacy.android.domain.entity.node

/**
 * Node name collision result
 *
 * @property noConflictNodes map of node handle and target handle to copy/move
 * @property conflictNodes map of node handle and [NodeNameCollision]
 * @property type [NodeNameCollisionType]
 */
data class NodeNameCollisionResult(
    val noConflictNodes: Map<Long, Long>,
    val conflictNodes: Map<Long, NodeNameCollision>,
    val type: NodeNameCollisionType,
)