package mega.privacy.android.app.usecase

import android.content.Context
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.rx3.rxSingle
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionResult
import mega.privacy.android.app.presentation.movenode.MoveRequestResult
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.app.usecase.exception.NotEnoughQuotaMegaException
import mega.privacy.android.app.usecase.exception.QuotaExceededMegaException
import mega.privacy.android.app.usecase.exception.SuccessMegaException
import mega.privacy.android.app.usecase.exception.toMegaException
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.filenode.MoveNodeToRubbishByHandle
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
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
    private val moveNodeToRubbishByHandle: MoveNodeToRubbishByHandle,
    private val accountRepository: AccountRepository,
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
            moveNodeToRubbishByHandle(NodeId(collisionResult.nameCollision.collisionHandle))
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
     * Moves nodes to a new location.
     *
     * @param handles           List of MegaNode handles to move.
     * @param newParentHandle   Parent MegaNode handle in which the nodes have to be moved.
     * @return The movement.
     */
    fun move(handles: LongArray, newParentHandle: Long): Single<MoveRequestResult.GeneralMovement> =
        rxSingle(ioDispatcher) {
            val parentNode = getMegaNode(newParentHandle)
                ?: throw MegaNodeException.ParentDoesNotExistException()

            var errorCount = 0
            val oldParentHandle = if (handles.size == 1) {
                getMegaNode(handles.first())?.parentHandle
            } else {
                INVALID_HANDLE
            }

            for (handle in handles) {
                val node = getMegaNode(handle)
                if (node == null) {
                    errorCount++
                } else {
                    runCatching {
                        moveAsync(node = node, parentNode = parentNode)
                    }.onFailure {
                        if (it.shouldEmmitError()) {
                            throw it
                        } else {
                            errorCount++
                        }
                    }
                }
            }

            MoveRequestResult.GeneralMovement(
                count = handles.size,
                errorCount = errorCount,
                oldParentHandle = oldParentHandle
            ).also { resetAccountDetailsIfNeeded(it) }
        }


    /**
     * Moves nodes to the Rubbish Bin.
     *
     * @param handles   List of MegaNode handles to move.
     * @return The movement result.
     */
    fun moveToRubbishBin(
        handles: List<Long>,
        context: Context,
    ) = rxSingle(ioDispatcher) {
        var errorCount = 0
        val rubbishNode = megaApiGateway.getRubbishBinNode()
        val oldParentHandle = if (handles.size == 1) {
            getMegaNode(handles.first())?.parentHandle
        } else {
            INVALID_HANDLE
        }
        handles.forEach { handle ->
            val node = getMegaNode(handle = handle)
            node?.let {
                runCatching {
                    moveAsync(node = node, parentNode = rubbishNode)
                }.onSuccess {
                    megaApiGateway.stopSharingNode(node)
                }.onFailure {
                    errorCount++
                }
            } ?: run { errorCount++ }
        }

        MoveRequestResult.RubbishMovement(
            count = handles.size,
            errorCount = errorCount,
            oldParentHandle = oldParentHandle,
            context = context
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

    /**
     * Resets the account details timestamp if some request finished with success.
     */
    private suspend fun resetAccountDetailsIfNeeded(request: MoveRequestResult) =
        withContext(ioDispatcher) {
            if (request.successCount > 0) {
                accountRepository.resetAccountDetailsTimeStamp()
            }
        }

    private suspend fun getMegaNode(handle: Long): MegaNode? = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(handle)
            ?: megaApiFolderGateway.getMegaNodeByHandle(handle)
                ?.let { megaApiFolderGateway.authorizeNode(it) }
    }

}