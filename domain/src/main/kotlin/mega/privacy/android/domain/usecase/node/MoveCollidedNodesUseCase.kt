package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.exception.extension.shouldEmitErrorForNodeMovement
import mega.privacy.android.domain.usecase.filenode.MoveNodeToRubbishBinUseCase
import javax.inject.Inject

/**
 * Moves the nodes of resolved name collisions to their destination.
 */
class MoveCollidedNodesUseCase @Inject constructor(
    private val moveNodeUseCase: MoveNodeUseCase,
    private val moveNodeToRubbishBinUseCase: MoveNodeToRubbishBinUseCase,
) {
    /**
     * Invoke
     *
     * @param nodeNameCollisions The collisions whose nodes to move; only nodes in the account tree
     * ([NodeNameCollision.Default]) can be moved.
     * @param rename True to move each node under its collision's rename name, false to replace the
     * colliding node.
     * @return How many nodes were and were not moved. A quota or foreign-node failure is thrown
     * instead of being counted, because it stops the whole batch.
     */
    suspend operator fun invoke(
        nodeNameCollisions: List<NodeNameCollision>,
        rename: Boolean,
    ): MoveRequestResult.GeneralMovement {
        val collisions = nodeNameCollisions.map { collision ->
            require(collision is NodeNameCollision.Default) {
                "Only nodes in the account tree can be moved: $collision"
            }
            collision
        }
        val results = coroutineScope {
            collisions.map { collision ->
                async {
                    runCatching { move(collision, rename) }
                        .onFailure { if (it.shouldEmitErrorForNodeMovement()) throw it }
                }
            }.awaitAll()
        }
        return MoveRequestResult.GeneralMovement(
            count = results.size,
            errorCount = results.count { it.isFailure },
        )
    }

    private suspend fun move(nodeNameCollision: NodeNameCollision.Default, rename: Boolean) {
        // Moving a node to the folder it already lives in is a no-op: the
        // colliding node is the node itself. Resolving such a collision by
        // sending the collision node to the rubbish bin would delete the very
        // node we are trying to move (see AND-23958).
        if (nodeNameCollision.collisionHandle == nodeNameCollision.nodeHandle) return
        if (!rename && nodeNameCollision.isFile) {
            moveNodeToRubbishBinUseCase(NodeId(nodeNameCollision.collisionHandle))
        }
        moveNodeUseCase(
            NodeId(nodeNameCollision.nodeHandle),
            NodeId(nodeNameCollision.parentHandle),
            if (rename) nodeNameCollision.renameName else null,
        )
    }
}
