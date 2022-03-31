package mega.privacy.android.app.usecase

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.blockingSubscribeBy
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.usecase.data.MoveRequestResult
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaError.API_EOVERQUOTA
import nz.mega.sdk.MegaError.API_OK
import nz.mega.sdk.MegaNode
import java.lang.IllegalArgumentException
import javax.inject.Inject

/**
 * Use case for moving MegaNodes.
 *
 * @property megaApi MegaApiAndroid instance to move nodes..
 */
class MoveNodeUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

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
                    if (error.errorCode == API_OK) {
                        emitter.onComplete()
                    } else {
                        emitter.onError(MegaException(error.errorCode, error.errorString))
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
            val parentNode = megaApi.getNodeByHandle(newParentHandle)

            if (parentNode == null) {
                emitter.onError(IllegalArgumentException("New parent node is not valid"))
            }

            var errorCount = 0
            var isForeignNode = false
            val oldParentHandle = if (handles.size == 1) {
                megaApi.getNodeByHandle(handles[0]).parentHandle
            } else {
                INVALID_HANDLE
            }

            handles.forEach { handle ->
                val node = megaApi.getNodeByHandle(handle)

                if (node == null) {
                    errorCount++
                } else {
                    move(node, parentNode).blockingSubscribeBy(onError = { error ->
                        errorCount++

                        if (error is MegaException && error.errorCode == API_EOVERQUOTA) {
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
                megaApi.getNodeByHandle(handles[0]).parentHandle
            } else {
                INVALID_HANDLE
            }

            handles.forEach { handle ->
                val node = megaApi.getNodeByHandle(handle)

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
                megaApi.getNodeByHandle(nodes[0].restoreHandle)
            } else {
                null
            }

            nodes.forEach { node ->
                val parent = megaApi.getNodeByHandle(node.restoreHandle)

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