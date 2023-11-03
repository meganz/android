package mega.privacy.android.app.usecase

import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.rx3.rxSingle
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionResult
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.app.usecase.exception.NotEnoughQuotaMegaException
import mega.privacy.android.app.usecase.exception.QuotaExceededMegaException
import mega.privacy.android.app.usecase.exception.SuccessMegaException
import mega.privacy.android.app.usecase.exception.toMegaException
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.filenode.MoveNodeToRubbishBinUseCase
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Use case for moving MegaNodes.
 *
 * @property megaApiGateway        MegaApiAndroid instance to move nodes.
 */
class LegacyMoveNodeUseCase @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val megaApiFolderGateway: MegaApiFolderGateway,
    private val moveNodeToRubbishBinUseCase: MoveNodeToRubbishBinUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * Moves node from one location to another
     *
     * @param node the node to be moved
     * @param parentNode parent location to which the nod should be moved to
     * @param newName if we should give a new name to the node at parentNode location
     */
    suspend fun moveAsync(
        node: MegaNode?,
        parentNode: MegaNode?,
        newName: String? = null,
    ): Boolean {
        node ?: throw MegaNodeException.NodeDoesNotExistsException()
        parentNode ?: throw MegaNodeException.ParentDoesNotExistException()
        val exception = suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    continuation.resumeWith(Result.success(error.toMegaException()))
                }
            )
            megaApiGateway.moveNode(
                nodeToMove = node,
                newNodeParent = parentNode,
                newNodeName = newName,
                listener = listener
            )
            continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
        }
        return when (exception) {
            is SuccessMegaException -> true
            is QuotaExceededMegaException -> {
                val isForeignNode = megaApiGateway.isForeignNode(parentNode.handle)
                if (isForeignNode) {
                    throw ForeignNodeException()
                } else {
                    throw exception
                }
            }

            else -> throw exception
        }
    }

    /**
     * Moves a node to other location after resolving a name collision.
     *
     * @param collisionResult   The result of the name collision.
     * @param rename            True if should rename the node, false otherwise.
     * @return Single with the movement result.
     */
    fun move(
        collisionResult: NameCollisionResult,
        rename: Boolean,
    ): Single<MoveRequestResult.GeneralMovement> = rxSingle(ioDispatcher) {
        moveAsync(collisionResult, rename)
    }

    /**
     * Moves a node to other location after resolving a name collision.
     *
     * @param collisionResult   The result of the name collision.
     * @param rename            True if should rename the node, false otherwise.
     * @return Single with the movement result.
     * */
    suspend fun moveAsync(
        collisionResult: NameCollisionResult,
        rename: Boolean,
    ): MoveRequestResult.GeneralMovement {
        val node =
            getMegaNode((collisionResult.nameCollision as NameCollision.Movement).nodeHandle)
                ?: throw MegaNodeException.NodeDoesNotExistsException()

        val parentNode = getMegaNode(collisionResult.nameCollision.parentHandle)
            ?: throw MegaNodeException.ParentDoesNotExistException()

        if (!rename && node.isFile) {
            moveNodeToRubbishBinUseCase(NodeId(collisionResult.nameCollision.collisionHandle))
        }
        val newName = if (rename) collisionResult.renameName else null
        return try {
            moveAsync(node, parentNode, newName)
            MoveRequestResult.GeneralMovement(count = 1, errorCount = 0)
        } catch (e: Exception) {
            if (e.shouldEmmitError()) {
                throw e
            } else {
                MoveRequestResult.GeneralMovement(count = 1, errorCount = 1)
            }
        }
    }

    /**
     * Moves a list of nodes to other location after resolving name collisions.
     *
     * @param collisions    The list with the result of the name collisions.
     * @param rename        True if should rename the nodes, false otherwise.
     * @return Single with the movements result.
     */
    fun move(
        collisions: List<NameCollisionResult>,
        rename: Boolean,
    ): Single<MoveRequestResult.GeneralMovement> = rxSingle(ioDispatcher) {
        var errorCount = 0
        for (collision in collisions) {
            runCatching {
                moveAsync(collision, rename)
            }.onFailure {
                if (it.shouldEmmitError()) {
                    throw it
                } else {
                    errorCount++
                }
            }
        }
        MoveRequestResult.GeneralMovement(
            count = collisions.size,
            errorCount = errorCount
        )
    }

    /**
     * Checks if the error of the request is over quota, pre over quota or foreign node.
     *
     * @return True if the error is one of those specified above, false otherwise.
     */
    private fun Throwable.shouldEmmitError(): Boolean =
        when (this) {
            is QuotaExceededMegaException, is NotEnoughQuotaMegaException, is ForeignNodeException -> true
            else -> false
        }

    private suspend fun getMegaNode(handle: Long): MegaNode? = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(handle)
            ?: megaApiFolderGateway.getMegaNodeByHandle(handle)
                ?.let { megaApiFolderGateway.authorizeNode(it) }
    }

}