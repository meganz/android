package mega.privacy.android.app.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.R
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.usecase.data.MoveRequestResult
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.DBUtil
import mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.MegaApiAndroid
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
    @MegaApi private val megaApi: MegaApiAndroid
) {

    /**
     * Moves nodes to a new location.
     *
     * @param handles           List of MegaNode handles to move.
     * @param newParentHandle   Parent MegaNode handle in which the nodes have to be moved.
     * @return The movement.
     */
    fun move(handles: LongArray, newParentHandle: Long): Single<MoveRequestResult> =
        Single.create { emitter ->
            val count = handles.size
            var pending = count
            var success = 0
            val oldParentHandle = if (count == 1) {
                megaApi.getNodeByHandle(handles[0]).parentHandle
            } else INVALID_VALUE.toLong()

            val listener =
                OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
                    pending--

                    if (error.errorCode == API_OK) {
                        success++
                    }

                    if (pending == 0) {
                        val errors = count - success
                        val foreignNode =
                            error.errorCode == API_EOVERQUOTA && megaApi.isForeignNode(request.parentHandle)

                        val result = when {
                            foreignNode -> {
                                MoveRequestResult(allSuccess = false, isForeignNode = true)
                            }
                            count == 1 && success == 1 -> {
                                MoveRequestResult(
                                    isSingleAction = true,
                                    oldParentHandle = oldParentHandle,
                                    resultText = getString(R.string.context_correctly_moved)
                                )
                            }
                            count == 1 && errors == 1 -> {
                                MoveRequestResult(
                                    isSingleAction = true,
                                    resultText = getString(R.string.context_no_moved),
                                    allSuccess = false
                                )
                            }
                            errors == 0 -> {
                                MoveRequestResult(
                                    resultText = getString(R.string.number_correctly_moved)
                                )
                            }
                            else -> {
                                val result = getString(R.string.number_correctly_moved, success) +
                                        getString(R.string.number_incorrectly_moved, error)

                                MoveRequestResult(
                                    resultText = result,
                                    allSuccess = false
                                )
                            }
                        }

                        DBUtil.resetAccountDetailsTimeStamp()
                        emitter.onSuccess(result)
                    }
                })

            for (handle in handles) {
                val node = megaApi.getNodeByHandle(handle)

                if (node == null) {
                    pending--
                    continue
                }

                megaApi.moveNode(node, megaApi.getNodeByHandle(newParentHandle), listener)
            }
        }

    /**
     * Moves nodes to the Rubbish Bin.
     *
     * @param handles   List of MegaNode handles to move.
     * @return The movement result.
     */
    fun moveToRubbishBin(handles: List<Long>): Single<MoveRequestResult> =
        Single.create { emitter ->
            val count = handles.size
            var pending = count
            var success = 0
            val oldParentHandle = if (count == 1) {
                megaApi.getNodeByHandle(handles[0]).parentHandle
            } else INVALID_VALUE.toLong()

            val listener =
                OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
                    pending--

                    if (error.errorCode == API_OK) {
                        success++
                    }

                    if (pending == 0) {
                        val errors = count - success
                        val foreignNode =
                            error.errorCode == API_EOVERQUOTA && megaApi.isForeignNode(request.parentHandle)

                        val result = when {
                            foreignNode -> {
                                MoveRequestResult(allSuccess = false, isForeignNode = true)
                            }
                            count == 1 && success == 1 -> {
                                DBUtil.resetAccountDetailsTimeStamp()
                                MoveRequestResult(
                                    isSingleAction = true,
                                    oldParentHandle = oldParentHandle,
                                    resultText = getString(R.string.context_correctly_moved_to_rubbish)
                                )
                            }
                            count == 1 && errors == 1 -> {
                                MoveRequestResult(
                                    isSingleAction = true,
                                    resultText = getString(R.string.context_no_moved),
                                    allSuccess = false
                                )
                            }
                            errors == 0 -> {
                                DBUtil.resetAccountDetailsTimeStamp()
                                MoveRequestResult(
                                    resultText = getQuantityString(
                                        R.plurals.number_correctly_moved_to_rubbish,
                                        success,
                                        success
                                    )
                                )
                            }
                            success == 0 -> {
                                MoveRequestResult(
                                    resultText = getQuantityString(
                                        R.plurals.number_incorrectly_moved_to_rubbish,
                                        errors,
                                        errors
                                    ),
                                    allSuccess = false
                                )
                            }
                            errors == 1 && success == 1 -> {
                                DBUtil.resetAccountDetailsTimeStamp()
                                MoveRequestResult(
                                    resultText =
                                    getString(R.string.node_correctly_and_node_incorrectly_moved_to_rubbish),
                                    allSuccess = false
                                )
                            }
                            errors == 1 -> {
                                DBUtil.resetAccountDetailsTimeStamp()
                                MoveRequestResult(
                                    resultText = getString(
                                        R.string.nodes_correctly_and_node_incorrectly_moved_to_rubbish,
                                        success
                                    ),
                                    allSuccess = false
                                )
                            }
                            success == 1 -> {
                                DBUtil.resetAccountDetailsTimeStamp()
                                MoveRequestResult(
                                    resultText = getString(
                                        R.string.node_correctly_and_nodes_incorrectly_moved_to_rubbish,
                                        errors
                                    ),
                                    allSuccess = false
                                )
                            }
                            else -> {
                                DBUtil.resetAccountDetailsTimeStamp()
                                MoveRequestResult(
                                    resultText = getString(
                                        R.string.nodes_correctly_and_nodes_incorrectly_moved_to_rubbish,
                                        success,
                                        errors
                                    ),
                                    allSuccess = false
                                )
                            }
                        }

                        emitter.onSuccess(result)
                    }
                })

            for (handle in handles) {
                val node = megaApi.getNodeByHandle(handle)

                if (node == null) {
                    pending--
                    continue
                }

                megaApi.moveNode(node, megaApi.rubbishNode, listener)
            }
        }

    /**
     * Moves nodes from the Rubbish Bin to their original parent if it still exists.
     *
     * @param nodes List of MegaNode to restore.
     * @return The restoration result.
     */
    fun restore(nodes: List<MegaNode>): Single<MoveRequestResult> =
        Single.create { emitter ->
            val count = nodes.size
            var pending = count
            var success = 0
            val listener =
                OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
                    pending--

                    if (error.errorCode == API_OK) {
                        success++
                    }

                    if (pending == 0) {
                        val errors = count - success
                        val foreignNode =
                            error.errorCode == API_EOVERQUOTA && megaApi.isForeignNode(request.parentHandle)

                        val result = when {
                            foreignNode -> {
                                MoveRequestResult(allSuccess = false, isForeignNode = true)
                            }
                            count == 1 && success == 1 -> {
                                DBUtil.resetAccountDetailsTimeStamp()
                                val destination = megaApi.getNodeByHandle(request.parentHandle)
                                MoveRequestResult(
                                    isSingleAction = true,
                                    resultText = getString(
                                        R.string.context_correctly_node_restored,
                                        destination.name
                                    )
                                )
                            }
                            count == 1 && errors == 1 -> {
                                MoveRequestResult(
                                    isSingleAction = true,
                                    resultText = getString(R.string.context_no_restored),
                                    allSuccess = false
                                )
                            }

                            errors == 0 -> {
                                DBUtil.resetAccountDetailsTimeStamp()
                                MoveRequestResult(
                                    resultText = getQuantityString(
                                        R.plurals.number_correctly_restored_from_rubbish,
                                        success,
                                        success
                                    )
                                )
                            }
                            success == 0 -> {
                                MoveRequestResult(
                                    resultText = getQuantityString(
                                        R.plurals.number_incorrectly_restored_from_rubbish,
                                        errors,
                                        errors
                                    ),
                                    allSuccess = false
                                )

                            }
                            errors == 1 && success == 1 -> {
                                DBUtil.resetAccountDetailsTimeStamp()
                                MoveRequestResult(
                                    resultText =
                                    getString(R.string.node_correctly_and_node_incorrectly_restored_from_rubbish),
                                    allSuccess = false
                                )
                            }
                            errors == 1 -> {
                                DBUtil.resetAccountDetailsTimeStamp()
                                MoveRequestResult(
                                    resultText = getString(
                                        R.string.nodes_correctly_and_node_incorrectly_restored_from_rubbish,
                                        success
                                    ),
                                    allSuccess = false
                                )
                            }
                            success == 1 -> {
                                DBUtil.resetAccountDetailsTimeStamp()
                                MoveRequestResult(
                                    resultText = getString(
                                        R.string.node_correctly_and_nodes_incorrectly_restored_from_rubbish,
                                        errors
                                    ),
                                    allSuccess = false
                                )
                            }
                            else -> {
                                DBUtil.resetAccountDetailsTimeStamp()
                                MoveRequestResult(
                                    resultText = getString(
                                        R.string.nodes_correctly_and_nodes_incorrectly_restored_from_rubbish,
                                        success,
                                        errors
                                    ),
                                    allSuccess = false
                                )
                            }
                        }

                        emitter.onSuccess(result)
                    }
                })

            for (node in nodes) {
                val parent = megaApi.getNodeByHandle(node.restoreHandle)

                if (parent == null) {
                    pending--
                    continue
                }

                megaApi.moveNode(node, parent, listener)
            }
        }
}