package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionWithActionResult
import mega.privacy.android.domain.entity.node.toMovementResult
import javax.inject.Inject

/**
 * Use case to check nodes name collision and copy/move/restore nodes which have no name collision
 */
class CheckNodesNameCollisionWithActionUseCase @Inject constructor(
    private val checkNodesNameCollisionUseCase: CheckNodesNameCollisionUseCase,
    private val copyNodesUseCase: CopyNodesUseCase,
    private val moveNodesUseCase: MoveNodesUseCase,
    private val restoreNodesUseCase: RestoreNodesUseCase,
) {
    /**
     * Invoke
     *
     * @param nodes map of node handle and target handle to copy/move/restore
     * @param type [NodeNameCollisionType]
     */
    suspend operator fun invoke(
        nodes: Map<Long, Long>,
        type: NodeNameCollisionType,
    ): NodeNameCollisionWithActionResult {
        val collisionResult = checkNodesNameCollisionUseCase(
            nodes = nodes,
            type = type
        )

        val movementResult = if (collisionResult.noConflictNodes.isNotEmpty()) {
            when (type) {
                NodeNameCollisionType.COPY -> copyNodesUseCase(collisionResult.noConflictNodes)
                NodeNameCollisionType.MOVE -> moveNodesUseCase(collisionResult.noConflictNodes)
                NodeNameCollisionType.RESTORE -> restoreNodesUseCase(collisionResult.noConflictNodes).toMovementResult()
            }
        } else null

        return NodeNameCollisionWithActionResult(
            collisionResult = collisionResult,
            moveRequestResult = movementResult
        )
    }
}
