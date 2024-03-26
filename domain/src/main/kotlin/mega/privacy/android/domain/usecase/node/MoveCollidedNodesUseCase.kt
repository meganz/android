package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.exception.extension.shouldEmitErrorForNodeMovement
import javax.inject.Inject

/**
 * Use Case to move name-collided nodes
 *
 */
class MoveCollidedNodesUseCase @Inject constructor(
    private val moveCollidedNodeUseCase: MoveCollidedNodeUseCase,
) {
    /**
     * Moves nodes to other location after resolving name collisions.
     * @param nodeNameCollisions list of [NodeNameCollision] that we want to move
     * @param rename [Boolean] true if the node should be renamed
     *
     * @return [MoveRequestResult]
     */
    suspend operator fun invoke(
        nodeNameCollisions: List<NodeNameCollision>,
        rename: Boolean,
    ): MoveRequestResult.GeneralMovement {
        val results = coroutineScope {
            nodeNameCollisions.map { nodeNameCollision ->
                async {
                    runCatching {
                        moveCollidedNodeUseCase(nodeNameCollision, rename)
                    }.recover {
                        if (it.shouldEmitErrorForNodeMovement()) throw it
                        return@async Result.failure(it)
                    }
                }
            }
        }.awaitAll()
        return MoveRequestResult.GeneralMovement(
            count = results.size,
            errorCount = results.count { it.isFailure },
        )
    }
}