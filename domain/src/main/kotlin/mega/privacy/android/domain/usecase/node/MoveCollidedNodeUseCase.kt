package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.exception.extension.shouldEmitErrorForNodeMovement
import mega.privacy.android.domain.usecase.filenode.MoveNodeToRubbishBinUseCase
import javax.inject.Inject

/**
 * Use Case to move name-collided node
 */
class MoveCollidedNodeUseCase @Inject constructor(
    private val moveNodeUseCase: MoveNodeUseCase,
    private val moveNodeToRubbishBinUseCase: MoveNodeToRubbishBinUseCase,
) {
    /**
     * Moves a node to other location after resolving a name collision.
     * @param nodeNameCollision the [NodeNameCollision] that we want to move
     * @param rename [Boolean] true if the node should be renamed
     *
     * @return [MoveRequestResult]
     */
    suspend operator fun invoke(
        nodeNameCollision: NodeNameCollision,
        rename: Boolean,
    ): MoveRequestResult.GeneralMovement {
        // Moving a node to the folder it already lives in is a no-op: the
        // colliding node is the node itself. Resolving such a collision by
        // sending the collision node to the rubbish bin would delete the very
        // node we are trying to move (see AND-23958).
        if (nodeNameCollision.collisionHandle == nodeNameCollision.nodeHandle) {
            return MoveRequestResult.GeneralMovement(count = 1, errorCount = 0)
        }
        if (!rename && nodeNameCollision.isFile) {
            moveNodeToRubbishBinUseCase(NodeId(nodeNameCollision.collisionHandle))
        }
        runCatching {
            moveNodeUseCase(
                NodeId(nodeNameCollision.nodeHandle),
                NodeId(nodeNameCollision.parentHandle),
                if (rename) nodeNameCollision.renameName else null
            )
        }.onSuccess {
            return MoveRequestResult.GeneralMovement(count = 1, errorCount = 0)
        }.recover {
            if (it.shouldEmitErrorForNodeMovement()) throw it
        }
        return MoveRequestResult.GeneralMovement(count = 1, errorCount = 1)
    }
}