package mega.privacy.android.app.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.R
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
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
     * @param handles       List of MegaNode handles to move.
     * @param parentNode    Parent MegaNode in which the nodes have to be moved.
     * @return The movement.
     */
    fun move(handles: List<Long>, parentNode: MegaNode): Single<String> =
        Single.create { emitter ->
            val count = handles.size
            var pending = count
            var success = 0
            val listener =
                OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
                    if (error.errorCode == API_OK) {
                        success++
                    }

                    if (pending == 0) {
                        val errors = count - success
                        val foreignNode =
                            error.errorCode == API_EOVERQUOTA && megaApi.isForeignNode(request.parentHandle)

                        val message: String = when {
                            count == 1 && success == 1 -> {
                                getString(R.string.context_correctly_moved)
                            }
                            count == 1 && errors == 1 -> {
                                if (foreignNode) "" else getString(R.string.context_no_moved)
                            }
                            errors == 0 -> {
                                getString(R.string.number_correctly_moved)
                            }
                            foreignNode -> {
                                ""
                            }
                            else -> {
                                getString(R.string.number_correctly_moved, success) +
                                        getString(R.string.number_incorrectly_moved, error)
                            }
                        }

                        DBUtil.resetAccountDetailsTimeStamp()
                        emitter.onSuccess(message)
                    }
                })

            for (handle in handles) {
                val node = megaApi.getNodeByHandle(handle)

                if (node == null) {
                    pending--
                    continue
                }

                megaApi.moveNode(node, parentNode, listener)
            }
        }

    /**
     * Moves nodes to the Rubbish Bin.
     *
     * @param handles   List of MegaNode handles to move.
     * @return The movement result.
     */
    fun moveToRubbishBin(handles: List<Long>): Single<String> =
        Single.create { emitter ->
            val count = handles.size
            var pending = count
            var success = 0
            val listener =
                OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
                    if (error.errorCode == API_OK) {
                        success++
                    }

                    if (pending == 0) {
                        val errors = count - success
                        val foreignNode =
                            error.errorCode == API_EOVERQUOTA && megaApi.isForeignNode(request.parentHandle)

                        val message: String = when {
                            count == 1 && success == 1 -> {
                                getString(R.string.context_correctly_moved_to_rubbish)
                            }
                            count == 1 && errors == 1 -> {
                                if (foreignNode) "" else getString(R.string.context_no_moved)
                            }
                            errors == 0 -> {
                                getQuantityString(
                                    R.plurals.number_correctly_moved_to_rubbish,
                                    success,
                                    success
                                )
                            }
                            success == 0 -> {
                                getQuantityString(
                                    R.plurals.number_incorrectly_moved_to_rubbish,
                                    errors,
                                    errors
                                )
                            }
                            errors == 1 && success == 1 -> {
                                getString(R.string.node_correctly_and_node_incorrectly_moved_to_rubbish)
                            }
                            errors == 1 -> {
                                getString(
                                    R.string.nodes_correctly_and_node_incorrectly_moved_to_rubbish,
                                    success
                                )
                            }
                            success == 1 -> {
                                getString(
                                    R.string.node_correctly_and_nodes_incorrectly_moved_to_rubbish,
                                    errors
                                )
                            }
                            else -> {
                                getString(
                                    R.string.nodes_correctly_and_nodes_incorrectly_moved_to_rubbish,
                                    success,
                                    errors
                                )
                            }
                        }

                        DBUtil.resetAccountDetailsTimeStamp()
                        emitter.onSuccess(message)
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
     * @param handles   List of MegaNode handles to restore.
     * @return The restoration result.
     */
    fun restore(handles: List<Long>): Single<String> =
        Single.create { emitter ->
            val count = handles.size
            var pending = count
            var success = 0
            val listener =
                OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
                    if (error.errorCode == API_OK) {
                        success++
                    }

                    if (pending == 0) {
                        val errors = count - success
                        val foreignNode =
                            error.errorCode == API_EOVERQUOTA && megaApi.isForeignNode(request.parentHandle)

                        val message: String = when {
                            count == 1 && success == 1 -> {
                                val destination = megaApi.getNodeByHandle(request.parentHandle)
                                getString(
                                    R.string.context_correctly_node_restored,
                                    destination.name
                                )
                            }
                            count == 1 && errors == 1 -> {
                                if (foreignNode) "" else getString(R.string.context_no_restored)
                            }

                            errors == 0 -> {
                                getQuantityString(
                                    R.plurals.number_correctly_restored_from_rubbish,
                                    success,
                                    success
                                )
                            }
                            success == 0 -> {
                                getQuantityString(
                                    R.plurals.number_incorrectly_restored_from_rubbish,
                                    errors,
                                    errors
                                )

                            }
                            errors == 1 && success == 1 -> {
                                getString(R.string.node_correctly_and_node_incorrectly_restored_from_rubbish)
                            }
                            errors == 1 -> {
                                getString(
                                    R.string.nodes_correctly_and_node_incorrectly_restored_from_rubbish,
                                    success
                                )
                            }
                            success == 1 -> {
                                getString(
                                    R.string.node_correctly_and_nodes_incorrectly_restored_from_rubbish,
                                    errors
                                )
                            }
                            else -> {
                                getString(
                                    R.string.nodes_correctly_and_nodes_incorrectly_restored_from_rubbish,
                                    success,
                                    errors
                                )
                            }
                        }

                        DBUtil.resetAccountDetailsTimeStamp()
                        emitter.onSuccess(message)
                    }
                })

            for (handle in handles) {
                val node = megaApi.getNodeByHandle(handle)

                if (node == null) {
                    pending--
                    continue
                }

                val parent = megaApi.getNodeByHandle(node.restoreHandle)

                if (parent == null) {
                    pending--
                    continue
                }

                megaApi.moveNode(node, parent, listener)
            }
        }
}