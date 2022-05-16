package mega.privacy.android.app.usecase

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.blockingSubscribeBy
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.usecase.data.MoveRequestResult
import mega.privacy.android.app.utils.RxUtil.blockingGetOrNull
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaError.API_EOVERQUOTA
import nz.mega.sdk.MegaError.API_OK
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Use case for moving MegaNodes.
 *
 * @property megaApi MegaApiAndroid instance to move nodes..
 */
class MoveNodeUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val getNodeUseCase: GetNodeUseCase
) {

    /**
     * Moves a node to other location.
     *
     * @param nodeHandle        Node handle to be moved
     * @param toParentHandle    Parent node handle to be moved to
     * @return                  Completable
     */
    fun move(nodeHandle: Long, toParentHandle: Long): Completable =
        Single.fromCallable {
            getNodeUseCase.get(nodeHandle).blockingGet() to getNodeUseCase.get(toParentHandle).blockingGet()
        }.flatMapCompletable { result -> move(result.first, result.second) }

    /**
     * Moves a node to other location.
     *
     * @param node          The MegaNoe to move.
     * @param parentNode    The parent MegaNode where the node has to be moved.
     * @return Completable.
     */
    fun move(node: MegaNode, parentNode: MegaNode): Completable =
        Completable.create { emitter ->
            megaApi.moveNode(
                node,
                parentNode,
                OptionalMegaRequestListenerInterface(onRequestFinish = { _, error ->
                    if (emitter.isDisposed) return@OptionalMegaRequestListenerInterface

                    if (error.errorCode == API_OK) {
                        emitter.onComplete()
                    } else {
                        emitter.onError(error.toMegaException())
                    }
                })
            )
        }

    /**
     * Moves nodes to a new location.
     *
     * @param handles           List of MegaNode handles to move.
     * @param newParentHandle   Parent MegaNode handle in which the nodes have to be moved.
     * @return The movement.
     */
    fun move(handles: LongArray, newParentHandle: Long): Single<MoveRequestResult> =
        Single.create { emitter ->
            val parentNode = getNodeUseCase.get(newParentHandle).blockingGet()

            var errorCount = 0
            var isForeignNode = false
            val oldParentHandle = if (handles.size == 1) {
                getNodeUseCase.get(handles.first()).blockingGet().parentHandle
            } else {
                INVALID_HANDLE
            }

            handles.forEach { handle ->
                val node = getNodeUseCase.get(handle).blockingGetOrNull()
                if (node == null) {
                    errorCount++
                } else {
                    move(node, parentNode).blockingSubscribeBy(onError = { error ->
                        errorCount++

                        if (error is QuotaExceededMegaException) {
                            isForeignNode = megaApi.isForeignNode(parentNode.handle)
                        }
                    })
                }
            }

            emitter.onSuccess(MoveRequestResult.GeneralMovement(
                handles.size,
                errorCount,
                oldParentHandle,
                isForeignNode
            ).apply { resetAccountDetailsIfNeeded() })
        }

    /**
     * Move a node to the Rubbish bin
     *
     * @param nodeHandle    Node handle to be moved
     * @return              Completable
     */
    fun moveToRubbishBin(nodeHandle: Long): Completable =
        Single.fromCallable { requireNotNull(megaApi.rubbishNode.handle) }
            .flatMapCompletable { rubbishModeHandle -> move(nodeHandle, rubbishModeHandle) }

    /**
     * Moves nodes to the Rubbish Bin.
     *
     * @param handles   List of MegaNode handles to move.
     * @return The movement result.
     */
    fun moveToRubbishBin(handles: List<Long>): Single<MoveRequestResult> =
        Single.create { emitter ->
            var errorCount = 0
            val rubbishNode = megaApi.rubbishNode
            val oldParentHandle = if (handles.size == 1) {
                getNodeUseCase.get(handles.first()).blockingGet().parentHandle
            } else {
                INVALID_HANDLE
            }

            handles.forEach { handle ->
                val node = getNodeUseCase.get(handle).blockingGetOrNull()
                if (node == null) {
                    errorCount++
                } else {
                    move(node, rubbishNode).blockingSubscribeBy(onError = {
                        errorCount++
                    })
                    megaApi.disableExport(node);
                }
            }

            emitter.onSuccess(MoveRequestResult.RubbishMovement(
                handles.size,
                errorCount,
                oldParentHandle
            ).apply { resetAccountDetailsIfNeeded() })
        }

    /**
     * Copy node to a different location, either passing handles or node itself.
     *
     * @param nodeHandle        Node handle to be copied
     * @param toParentHandle    Parent node handle to be copied to
     * @param node              Node to be copied
     * @param toParentNode      Parent node to be copied to
     * @return                  Completable
     */
    fun copyNode(
        nodeHandle: Long? = null,
        toParentHandle: Long? = null,
        node: MegaNode? = null,
        toParentNode: MegaNode? = null
    ): Completable =
        Completable.fromCallable {
            require((node != null || nodeHandle != null) && (toParentNode != null || toParentHandle != null))
            copyNode(
                node ?: getNodeUseCase.get(nodeHandle!!).blockingGet(),
                toParentNode ?: getNodeUseCase.get(toParentHandle!!).blockingGet()
            ).blockingAwait()
        }

    /**
     * Copy node to a different location.
     *
     * @param currentNode   Node to be copied
     * @param toParentNode  Parent node to be copied to
     * @return              Completable
     */
    fun copyNode(currentNode: MegaNode?, toParentNode: MegaNode?): Completable =
        Completable.create { emitter ->
            if (currentNode == null || toParentNode == null) {
                emitter.onError(IllegalArgumentException("Null nodes"))
                return@create
            }

            megaApi.copyNode(currentNode, toParentNode, OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    if (emitter.isDisposed) return@OptionalMegaRequestListenerInterface

                    if (error.errorCode == MegaError.API_OK) {
                        emitter.onComplete()
                    } else {
                        emitter.onError(error.toMegaException())
                    }
                }
            ))
        }

    /**
     * Moves nodes from the Rubbish Bin to their original parent if it still exists.
     *
     * @param nodes List of MegaNode to restore.
     * @return The restoration result.
     */
    fun restore(nodes: List<MegaNode>): Single<MoveRequestResult> =
        Single.create { emitter ->
            var errorCount = 0
            var isForeignNode = false
            val destination: MegaNode? = if (nodes.size == 1) {
                getNodeUseCase.get(nodes.first().restoreHandle).blockingGet()
            } else {
                null
            }

            nodes.forEach { node ->
                val parent = getNodeUseCase.get(node.restoreHandle).blockingGetOrNull()
                if (parent == null) {
                    errorCount++
                } else {
                    move(node, parent).blockingSubscribeBy(onError = { error ->
                        errorCount++

                        if (error is MegaException && error.errorCode == API_EOVERQUOTA) {
                            isForeignNode = megaApi.isForeignNode(node.restoreHandle)
                        }
                    })
                }
            }

            emitter.onSuccess(MoveRequestResult.Restoration(
                nodes.size,
                errorCount,
                isForeignNode,
                destination
            ).apply { resetAccountDetailsIfNeeded() })
        }
}
