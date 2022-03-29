package mega.privacy.android.app.usecase

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.blockingSubscribeBy
import mega.privacy.android.app.R
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.usecase.exception.BusinessAccountOverdueException
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionResult
import mega.privacy.android.app.usecase.data.MoveRequestResult
import mega.privacy.android.app.usecase.exception.ForeignNodeException
import mega.privacy.android.app.usecase.exception.MegaException
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.app.utils.RxUtil.blockingGetOrNull
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaError.*
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Use case for moving MegaNodes.
 *
 * @property megaApi        MegaApiAndroid instance to move nodes.
 * @property getNodeUseCase Required for getting MegaNodes.
 */
class MoveNodeUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val getNodeUseCase: GetNodeUseCase
) {

    /**
     * Moves a node to other location.
     *
     * @param handle        The identifier of the MegaNode to move.
     * @param parentHandle  The parent MegaNode where the node has to be moved.
     * @return Completable.
     */
    fun move(handle: Long, parentHandle: Long): Completable =
        Completable.fromCallable {
            move(
                getNodeUseCase.get(handle).blockingGetOrNull(),
                getNodeUseCase.get(parentHandle).blockingGetOrNull()
            ).blockingAwait()
        }

    /**
     * Moves a node to other location.
     *
     * @param node          MegaNode to move.
     * @param parentHandle  The parent MegaNode where the node has to be moved.
     * @return Completable.
     */
    fun move(node: MegaNode, parentHandle: Long): Completable =
        Completable.fromCallable {
            move(node, getNodeUseCase.get(parentHandle).blockingGetOrNull()).blockingAwait()
        }

    /**
     * Moves a node to the Rubbish Bin.
     *
     * @param handle        The identifier of the MegaNode to move.
     * @return Completable.
     */
    fun moveToRubbishBin(handle: Long): Completable =
        Completable.fromCallable {
            move(getNodeUseCase.get(handle).blockingGetOrNull(), megaApi.rubbishNode)
                .blockingAwait()
        }

    /**
     * Moves a node to other location.
     *
     * @param node          The MegaNoe to move.
     * @param parentNode    The parent MegaNode where the node has to be moved.
     * @param newName       New name for the moved node. Null if it wants to keep the original one.
     * @return Completable.
     */
    private fun move(node: MegaNode?, parentNode: MegaNode?, newName: String? = null): Completable =
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
                    else ->
                        if (error.errorCode == API_EOVERQUOTA
                            && megaApi.isForeignNode(parentNode.handle)
                        ) {
                            emitter.onError(ForeignNodeException())
                        } else {
                            emitter.onError(
                                MegaException(
                                    error.errorCode,
                                    getString(R.string.context_no_moved)
                                )
                            )
                        }
                }
            })

            if (newName != null) {
                megaApi.moveNode(node, parentNode, newName, listener)
            } else {
                megaApi.moveNode(node, parentNode, listener)
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
        rename: Boolean
    ): Single<MoveRequestResult.GeneralMovement> =
        Single.create { emitter ->
            val node = getNodeUseCase
                .get((collisionResult.nameCollision as NameCollision.Movement).nodeHandle)
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
                moveToRubbishBin(collisionResult.nameCollision.collisionHandle)
                    .blockingSubscribeBy(onError = { error -> emitter.onError(error) })
            }

            if (emitter.isDisposed) {
                return@create
            }

            move(node, parentNode, if (rename) collisionResult.renameName else null)
                .blockingSubscribeBy(
                    onError = { error ->
                        if (emitter.isDisposed) {
                            return@blockingSubscribeBy
                        }

                        emitter.onSuccess(
                            MoveRequestResult.GeneralMovement(
                                count = 1,
                                errorCount = 1,
                                isForeignNode = error is MegaException && error.errorCode == API_EOVERQUOTA
                            )
                        )
                    },
                    onComplete = {
                        if (emitter.isDisposed) {
                            return@blockingSubscribeBy
                        }

                        emitter.onSuccess(
                            MoveRequestResult.GeneralMovement(
                                count = 1,
                                errorCount = 0,
                                isForeignNode = false
                            )
                        )
                    }
                )
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
        rename: Boolean
    ): Single<MoveRequestResult.GeneralMovement> =
        Single.create { emitter ->
            var errorCount = 0
            var isForeignNode = false

            collisions.forEach { collision ->
                if (emitter.isDisposed) {
                    return@create
                }

                move(collision, rename).blockingSubscribeBy(onError = { error ->
                    errorCount++
                    isForeignNode = error is ForeignNodeException
                })
            }

            when {
                emitter.isDisposed -> return@create
                else -> emitter.onSuccess(
                    MoveRequestResult.GeneralMovement(
                        count = collisions.size,
                        errorCount = errorCount,
                        isForeignNode = isForeignNode
                    )
                )
            }
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
            val parentNode = getNodeUseCase.get(newParentHandle).blockingGetOrNull()

            if (parentNode == null) {
                emitter.onError(MegaNodeException.ParentDoesNotExistException())
                return@create
            }

            var errorCount = 0
            var isForeignNode = false
            val oldParentHandle = if (handles.size == 1) {
                getNodeUseCase.get(handles[0]).blockingGetOrNull()?.parentHandle
            } else {
                INVALID_HANDLE
            }

            handles.forEach { handle ->
                val node = getNodeUseCase.get(handle).blockingGetOrNull()

                if (node == null) {
                    errorCount++
                } else {
                    move(node, parentNode)
                        .blockingSubscribeBy(onError = { error ->
                            errorCount++
                            isForeignNode = error is ForeignNodeException
                        })
                }
            }

            if (emitter.isDisposed) {
                return@create
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
                getNodeUseCase.get(handles[0]).blockingGetOrNull()?.parentHandle
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

            if (emitter.isDisposed) {
                return@create
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
                getNodeUseCase.get(nodes[0].restoreHandle).blockingGetOrNull()
            } else {
                null
            }

            nodes.forEach { node ->
                val parent = getNodeUseCase.get(node.restoreHandle).blockingGetOrNull()

                if (parent == null || megaApi.isInRubbish(parent)) {
                    errorCount++
                } else {
                    move(node, parent).blockingSubscribeBy(onError = { error ->
                        errorCount++
                        isForeignNode = error is ForeignNodeException
                    })
                }
            }

            if (emitter.isDisposed) {
                return@create
            }

            emitter.onSuccess(MoveRequestResult.Restoration(
                nodes.size,
                errorCount,
                isForeignNode,
                destination
            ).apply { resetAccountDetailsIfNeeded() })
        }
}