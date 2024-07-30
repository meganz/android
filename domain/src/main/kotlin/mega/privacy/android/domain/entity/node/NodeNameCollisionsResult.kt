package mega.privacy.android.domain.entity.node

/**
 * Node name collision result
 *
 * @property noConflictNodes map of node handle and target handle to copy/move
 * @property conflictNodes map of node handle and [NodeNameCollision]
 * @property type [NodeNameCollisionType]
 */
data class NodeNameCollisionsResult(
    val noConflictNodes: Map<Long, Long>,
    val conflictNodes: Map<Long, NodeNameCollision>,
    val type: NodeNameCollisionType,
)

/**
 * Node name collision with action result
 * @property collisionResult [NodeNameCollisionsResult]
 * @property moveRequestResult [MoveRequestResult] null if no action was performed
 */
data class NodeNameCollisionWithActionResult(
    val collisionResult: NodeNameCollisionsResult,
    val moveRequestResult: MoveRequestResult? = null,
) {
    /**
     * Get the first collision from result, null if there is no collision
     */
    val firstNodeCollisionOrNull: NodeNameCollision?
        get() = collisionResult.conflictNodes.values.firstOrNull()

    /**
     * Get the first chat node collision from result, null if there is no chat node collision
     */
    val firstChatNodeCollisionOrNull
        get() = firstNodeCollisionOrNull as? NodeNameCollision.Chat
}