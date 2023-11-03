package mega.privacy.android.app.usecase

import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.rx3.rxCompletable
import kotlinx.coroutines.rx3.rxSingle
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionResult
import mega.privacy.android.app.presentation.copynode.CopyRequestResult
import mega.privacy.android.app.usecase.chat.GetChatMessageUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.app.usecase.exception.NotEnoughQuotaMegaException
import mega.privacy.android.app.usecase.exception.QuotaExceededMegaException
import mega.privacy.android.app.usecase.exception.SuccessMegaException
import mega.privacy.android.app.usecase.exception.toMegaException
import mega.privacy.android.app.utils.RxUtil.blockingGetOrNull
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.filenode.MoveNodeToRubbishBinUseCase
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Use case for copying MegaNodes.
 *
 * @property megaApiGateway             MegaApiGateway instance to copy nodes.
 * @property getChatMessageUseCase      Required for getting chat [MegaNode]s.
 * @property copyNodeListUseCase        copy list of mega nodes
 * @property moveNodeToRubbishBinUseCase
 * @property ioDispatcher
 */
class LegacyCopyNodeUseCase @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val megaApiFolderGateway: MegaApiFolderGateway,
    private val getChatMessageUseCase: GetChatMessageUseCase,
    private val copyNodeListUseCase: CopyNodeListUseCase,
    private val moveNodeToRubbishBinUseCase: MoveNodeToRubbishBinUseCase,
    private val accountRepository: AccountRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * Copies a node.
     *
     * @param node          The MegaNode to copy.
     * @param parentHandle  The parent MegaNode where the node has to be copied.
     * @return Completable.
     */
    fun copy(node: MegaNode?, parentHandle: Long) = rxCompletable(ioDispatcher) {
        copyAsync(node, getMegaNode(parentHandle))
    }

    /**
     * Copies a node.
     *
     * @param node          The MegaNoe to copy.
     * @param parentNode    The parent MegaNode where the node has to be copied.
     * @param newName       New name for the copied node. Null if it wants to keep the original one.
     * @return Completable.
     */
    fun copy(node: MegaNode?, parentNode: MegaNode?, newName: String? = null) =
        rxCompletable(ioDispatcher) {
            copyAsync(node, parentNode, newName)
        }


    /**
     * Copies a node.
     *
     * @param node          The MegaNoe to copy.
     * @param parentNode    The parent MegaNode where the node has to be copied.
     * @param newName       New name for the copied node. Null if it wants to keep the original one.
     * @return CopyRequestResult.
     */
    suspend fun copyAsync(
        node: MegaNode?,
        parentNode: MegaNode?,
        newName: String? = null,
    ): Boolean {
        node ?: throw MegaNodeException.NodeDoesNotExistsException()
        parentNode ?: throw MegaNodeException.ParentDoesNotExistException()

        val response = suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    continuation.resumeWith(Result.success(error.toMegaException()))
                }
            )
            megaApiGateway.copyNode(
                nodeToCopy = node,
                newNodeParent = parentNode,
                newNodeName = newName,
                listener = listener
            )
            continuation.invokeOnCancellation { megaApiGateway.removeRequestListener(listener) }
        }
        return when (response) {
            is SuccessMegaException -> true
            is QuotaExceededMegaException -> {
                val isForeignNode = megaApiGateway.isForeignNode(parentNode.handle)
                if (isForeignNode) {
                    throw ForeignNodeException()
                } else {
                    throw response
                }
            }

            else -> throw response
        }
    }

    /**
     * Copies a node after resolving a name collision.
     *
     * @param collisionResult   The result of the name collision.
     * @param rename            True if should rename the node, false otherwise.
     * @return Single with the copy result.
     */
    fun copy(
        collisionResult: NameCollisionResult,
        rename: Boolean,
    ): Single<CopyRequestResult> = rxSingle(ioDispatcher) {
        copyAsync(collisionResult, rename)
    }

    /**
     * Copies a node after resolving a name collision.
     *
     * @param collisionResult   The result of the name collision.
     * @param rename            True if should rename the node, false otherwise.
     * @return copy result.
     */
    suspend fun copyAsync(
        collisionResult: NameCollisionResult,
        rename: Boolean,
    ): CopyRequestResult {
        val node = if (collisionResult.nameCollision is NameCollision.Import) {
            val collision = collisionResult.nameCollision
            val nodes =
                getChatMessageUseCase.getChatNodes(collision.chatId, collision.messageId)
                    .blockingGetOrNull()

            nodes?.let {
                var nodeCollision: MegaNode? = null

                for (node in nodes) {
                    if (node.handle == collision.nodeHandle) {
                        nodeCollision = node
                        break
                    }
                }

                nodeCollision
            }
        } else {
            getMegaNode((collisionResult.nameCollision as NameCollision.Copy).nodeHandle)
                ?: MegaNode.unserialize(collisionResult.nameCollision.serializedNode)
        } ?: throw MegaNodeException.NodeDoesNotExistsException()


        val parentNode = getMegaNode(collisionResult.nameCollision.parentHandle ?: -1)
            ?: throw MegaNodeException.ParentDoesNotExistException()

        if (!rename && node.isFile) {
            moveNodeToRubbishBinUseCase(NodeId(collisionResult.nameCollision.collisionHandle))
        }

        val newName = if (rename) collisionResult.renameName else null
        return try {
            copyAsync(node, parentNode, newName)
            CopyRequestResult(count = 1, errorCount = 0)
        } catch (exception: Exception) {
            if (exception.shouldEmmitError()) {
                throw exception
            } else {
                CopyRequestResult(count = 1, errorCount = 1)
            }
        }
    }

    private suspend fun getMegaNode(handle: Long): MegaNode? = withContext(ioDispatcher) {
        megaApiGateway.getMegaNodeByHandle(handle)
            ?: megaApiFolderGateway.getMegaNodeByHandle(handle)
                ?.let { megaApiFolderGateway.authorizeNode(it) }
    }


    /**
     * Copies a list of nodes after resolving name collisions.
     *
     * @param collisions    The list with the result of the name collisions.
     * @param rename        True if should rename the nodes, false otherwise.
     * @return Single with the copies result.
     */
    fun copy(
        collisions: List<NameCollisionResult>,
        rename: Boolean,
    ): Single<CopyRequestResult> = rxSingle(ioDispatcher) {
        var errorCount = 0
        for (collision in collisions) {
            runCatching {
                copyAsync(collision, rename)
            }.onFailure {
                if (it.shouldEmmitError()) {
                    throw it
                } else {
                    errorCount++
                }
            }
        }
        CopyRequestResult(
            count = collisions.size,
            errorCount = errorCount
        ).also { resetAccountDetailsIfNeeded(it) }
    }

    /**
     * Copies nodes.
     *
     * @param nodes         List of MegaNodes to copy.
     * @param parentHandle  Parent MegaNode handle in which the nodes have to be copied.
     * @return Single with the [CopyRequestResult].
     */
    fun copy(nodes: List<MegaNode>, parentHandle: Long): Single<CopyRequestResult> =
        rxSingle { copyNodeListUseCase(nodes, parentHandle) }

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
    private suspend fun resetAccountDetailsIfNeeded(request: CopyRequestResult) =
        withContext(ioDispatcher) {
            if (request.successCount > 0) {
                accountRepository.resetAccountDetailsTimeStamp()
            }
        }
}
