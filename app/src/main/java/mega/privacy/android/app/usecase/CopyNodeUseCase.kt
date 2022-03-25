package mega.privacy.android.app.usecase

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.blockingSubscribeBy
import mega.privacy.android.app.R
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionResult
import mega.privacy.android.app.usecase.data.CopyRequestResult
import mega.privacy.android.app.usecase.exception.*
import mega.privacy.android.app.utils.RxUtil.blockingGetOrNull
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.*
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaError.*
import javax.inject.Inject

/**
 * Use case for copying MegaNodes.
 *
 * @property megaApi            MegaApiAndroid instance to copy nodes.
 * @property megaChatApi        MegaChatApiAndroid instance to get nodes from chats.
 * @property getNodeUseCase     Required for getting MegaNodes.
 * @property moveNodeUseCase    Required for moving MegaNodes to the Rubbish Bin.
 */
class CopyNodeUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    private val getNodeUseCase: GetNodeUseCase,
    private val moveNodeUseCase: MoveNodeUseCase
) {

    /**
     * Copies a node.
     *
     * @param handle        The identifier of the MegaNode to copy.
     * @param parentHandle  The parent MegaNode where the node has to be copied.
     * @return Completable.
     */
    fun copy(handle: Long, parentHandle: Long): Completable =
        Completable.fromCallable {
            copy(
                getNodeUseCase.get(handle).blockingGetOrNull(),
                getNodeUseCase.get(parentHandle).blockingGetOrNull()
            ).blockingAwait()
        }

    /**
     * Copies a node.
     *
     * @param node          The MegaNode to copy.
     * @param parentNode    The parent MegaNode where the node has to be copied.
     * @return Completable.
     */
    fun copy(node: MegaNode, parentNode: MegaNode): Completable =
        Completable.fromCallable { copy(node, parentNode).blockingAwait() }

    /**
     * Copies a node.
     *
     * @param node          The MegaNode to copy.
     * @param parentHandle  The parent MegaNode where the node has to be copied.
     * @return Completable.
     */
    fun copy(node: MegaNode?, parentHandle: Long): Completable =
        Completable.fromCallable {
            copy(
                node,
                getNodeUseCase.get(parentHandle).blockingGetOrNull()
            ).blockingAwait()
        }

    /**
     * Copies a node.
     *
     * @param node          The MegaNoe to copy.
     * @param parentNode    The parent MegaNode where the node has to be copied.
     * @param newName       New name for the copied node. Null if it wants to keep the original one.
     * @return Completable.
     */
    fun copy(node: MegaNode?, parentNode: MegaNode?, newName: String? = null): Completable =
        Completable.create { emitter ->
            if (node == null) {
                emitter.onError(MegaNodeException.NodeDoesNotExistsException())
                return@create
            }

            if (parentNode == null) {
                emitter.onError(MegaNodeException.ParentDoesNotExistException())
                return@create
            }

            val listener = OptionalMegaRequestListenerInterface(onRequestFinish = { _, error ->
                if (emitter.isDisposed) {
                    return@OptionalMegaRequestListenerInterface
                }

                when (error.errorCode) {
                    API_OK -> emitter.onComplete()
                    API_EBUSINESSPASTDUE -> emitter.onError(BusinessAccountOverdueException())
                    API_EOVERQUOTA -> {
                        if (megaApi.isForeignNode(parentNode.handle)) {
                            emitter.onError(ForeignNodeException())
                        } else {
                            emitter.onError(OverQuotaException())
                        }
                    }
                    API_EGOINGOVERQUOTA -> emitter.onError(PreOverQuotaException())
                    else -> emitter.onError(
                        MegaException(
                            error.errorCode,
                            getString(R.string.context_no_copied)
                        )
                    )
                }
            })

            if (newName != null) {
                megaApi.copyNode(node, parentNode, newName, listener)
            } else {
                megaApi.copyNode(node, parentNode, listener)
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
        rename: Boolean
    ): Single<CopyRequestResult> =
        Single.create { emitter ->
            val node = getNodeUseCase
                .get((collisionResult.nameCollision as NameCollision.Copy).nodeHandle)
                .blockingGetOrNull()

            if (node == null) {
                emitter.onError(MegaNodeException.NodeDoesNotExistsException())
                return@create
            }

            val parentNode = getNodeUseCase
                .get(collisionResult.nameCollision.parentHandle).blockingGetOrNull()

            if (parentNode == null) {
                emitter.onError(MegaNodeException.ParentDoesNotExistException())
                return@create
            }

            if (!rename && node.isFile) {
                moveNodeUseCase.moveToRubbishBin(collisionResult.nameCollision.collisionHandle)
                    .blockingSubscribeBy(onError = { error -> emitter.onError(error) })
            }

            if (emitter.isDisposed) {
                return@create
            }

            copy(node, parentNode, if (rename) collisionResult.renameName else null)
                .blockingSubscribeBy(
                    onError = { error ->
                        if (emitter.isDisposed) {
                            return@blockingSubscribeBy
                        }

                        emitter.onSuccess(
                            CopyRequestResult(
                                count = 1,
                                errorCount = 1,
                                isForeignNode = error is MegaException && error.errorCode == MegaError.API_EOVERQUOTA
                            )
                        )
                    },
                    onComplete = {
                        if (emitter.isDisposed) {
                            return@blockingSubscribeBy
                        }

                        emitter.onSuccess(
                            CopyRequestResult(
                                count = 1,
                                errorCount = 0,
                                isForeignNode = false
                            ).apply { resetAccountDetailsIfNeeded() }
                        )
                    }
                )
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
        rename: Boolean
    ): Single<CopyRequestResult> =
        Single.create { emitter ->
            var errorCount = 0
            var isForeignNode = false

            collisions.forEach { collision ->
                if (emitter.isDisposed) {
                    return@create
                }

                copy(collision, rename).blockingSubscribeBy(onError = { error ->
                    errorCount++
                    isForeignNode = error is ForeignNodeException
                })
            }

            when {
                emitter.isDisposed -> return@create
                else -> emitter.onSuccess(
                    CopyRequestResult(
                        count = collisions.size,
                        errorCount = errorCount,
                        isForeignNode = isForeignNode
                    ).apply { resetAccountDetailsIfNeeded() }
                )
            }
        }

    /**
     * Copies nodes.
     *
     * @param handles           List of MegaNode handles to copy.
     * @param newParentHandle   Parent MegaNode handle in which the nodes have to be copied.
     * @return Single with the [CopyRequestResult].
     */
    fun copy(handles: LongArray, newParentHandle: Long): Single<CopyRequestResult> =
        Single.create { emitter ->
            val parentNode = getNodeUseCase.get(newParentHandle).blockingGetOrNull()

            if (parentNode == null) {
                emitter.onError(MegaNodeException.ParentDoesNotExistException())
                return@create
            }

            var errorCount = 0
            var isForeignNode = false

            handles.forEach { handle ->
                val node = getNodeUseCase.get(handle).blockingGetOrNull()

                if (node == null) {
                    errorCount++
                } else {
                    copy(node, parentNode)
                        .blockingSubscribeBy(onError = { error ->
                            errorCount++
                            isForeignNode = error is ForeignNodeException
                        })
                }
            }

            if (emitter.isDisposed) {
                return@create
            }

            emitter.onSuccess(
                CopyRequestResult(
                    handles.size,
                    errorCount,
                    isForeignNode
                ).apply { resetAccountDetailsIfNeeded() }
            )
        }

    /**
     * Copies nodes.
     *
     * @param nodes         List of MegaNodes to copy.
     * @param parentHandle  Parent MegaNode handle in which the nodes have to be copied.
     * @return Single with the [CopyRequestResult].
     */
    fun copy(nodes: List<MegaNode>, parentHandle: Long): Single<CopyRequestResult> =
        Single.create { emitter ->
            val parentNode = getNodeUseCase.get(parentHandle).blockingGetOrNull()

            if (parentNode == null) {
                emitter.onError(MegaNodeException.ParentDoesNotExistException())
                return@create
            }

            var errorCount = 0
            var isForeignNode = false

            nodes.forEach { node ->
                copy(node, parentNode)
                    .blockingSubscribeBy(onError = { error ->
                        errorCount++
                        isForeignNode = error is ForeignNodeException
                    })
            }

            if (emitter.isDisposed) {
                return@create
            }

            emitter.onSuccess(
                CopyRequestResult(
                    nodes.size,
                    errorCount,
                    isForeignNode
                ).apply { resetAccountDetailsIfNeeded() }
            )
        }

    /**
     * Imports/copies nodes from a chat conversation.
     *
     * @param messageIds    Array containing the message ids with the nodes to import.
     * @param chatId        Identifier of the chat from which the nodes will be imported.
     * @param parentHandle  Parent MegaNode handle in which the nodes have to be imported.
     * @return Single with the [CopyRequestResult].
     */
    fun import(
        messageIds: LongArray,
        chatId: Long,
        parentHandle: Long
    ): Single<CopyRequestResult> =
        Single.create { emitter ->
            if (chatId == MEGACHAT_INVALID_HANDLE || megaChatApi.getChatRoom(chatId) == null) {
                emitter.onError(ChatDoesNotExistException())
                return@create
            }

            val parentNode = getNodeUseCase.get(parentHandle).blockingGetOrNull()

            if (parentNode == null) {
                emitter.onError(MegaNodeException.ParentDoesNotExistException())
                return@create
            }

            if (messageIds.isEmpty()) {
                emitter.onError(MessageDoesNotExistException())
                return@create
            }

            var errorCount = 0
            var isForeignNode = false
            var nodesSize = 0

            messageIds.forEach { messageId ->
                val attachments = megaChatApi.getMessage(chatId, messageId)?.megaHandleList

                if (attachments != null) {
                    for (i in 0 until attachments.size()) {
                        nodesSize++
                        copy(attachments.get(i), parentHandle).blockingSubscribeBy(
                            onError = { error ->
                                errorCount++
                                isForeignNode = error is ForeignNodeException
                            }
                        )
                    }
                }
            }

            when {
                emitter.isDisposed -> return@create
                else -> emitter.onSuccess(
                    CopyRequestResult(
                        nodesSize,
                        errorCount,
                        isForeignNode
                    ).apply { resetAccountDetailsIfNeeded() }
                )
            }
        }
}