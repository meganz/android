package mega.privacy.android.app.presentation.movenode

import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.movenode.MoveRequestResult.GeneralMovement
import mega.privacy.android.app.presentation.movenode.MoveRequestResult.Restoration
import mega.privacy.android.app.presentation.movenode.MoveRequestResult.RubbishMovement
import mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode

/**
 * Sealed class containing all the info related to a movement request.
 * The class can be a [GeneralMovement], [RubbishMovement] or [Restoration]
 *
 * @property count              Number of requests.
 * @property errorCount         Number of requests which finished with an error.
 * @property oldParentHandle    Handle of the old parent.
 */
sealed class MoveRequestResult(
    val count: Int,
    val errorCount: Int,
    val oldParentHandle: Long? = MegaApiJava.INVALID_HANDLE,
) {
    /**
     * Count of success move request
     */
    val successCount = count - errorCount

    /**
     * Checks whether move node request has only one request
     */
    val isSingleAction: Boolean = count == 1

    /**
     * Checks whether all move node request are all success
     */
    val isSuccess: Boolean = errorCount == 0

    /**
     * Checks whether move node request has data
     */
    val hasNoData: Boolean = count == 0

    /**
     * Checks whether all move node request are errors
     */
    val isAllRequestError: Boolean = errorCount == count

    /**
     * Get Result Text Message after moving node
     */
    open fun getResultText(): String = ""

    /**
     * Result of a movement request from one location to another one.
     */
    class GeneralMovement(
        count: Int,
        errorCount: Int,
        oldParentHandle: Long? = null,
    ) : MoveRequestResult(count, errorCount, oldParentHandle)

    /**
     * Result of a movement to the Rubbish Bin.
     */
    class RubbishMovement constructor(
        count: Int,
        errorCount: Int,
        oldParentHandle: Long?,
    ) : MoveRequestResult(
        count = count,
        errorCount = errorCount,
        oldParentHandle = oldParentHandle
    ) {
        override fun getResultText(): String =
            when {
                count == 1 && isSuccess -> {
                    getString(R.string.context_correctly_moved_to_rubbish)
                }
                count == 1 -> {
                    getString(R.string.context_no_moved)
                }
                isSuccess -> {
                    getQuantityString(R.plurals.number_correctly_moved_to_rubbish, count, count)
                }
                count == errorCount -> {
                    getQuantityString(
                        R.plurals.number_incorrectly_moved_to_rubbish,
                        errorCount,
                        errorCount
                    )
                }
                errorCount == 1 -> {
                    getQuantityString(
                        R.plurals.nodes_correctly_and_node_incorrectly_moved_to_rubbish,
                        successCount,
                        successCount
                    )
                }
                successCount == 1 -> {
                    getQuantityString(
                        R.plurals.node_correctly_and_nodes_incorrectly_moved_to_rubbish,
                        errorCount,
                        errorCount
                    )
                }
                else -> {
                    StringBuilder().append(
                        getQuantityString(
                            R.plurals.number_correctly_moved_to_rubbish,
                            successCount,
                            successCount
                        )
                    ).append(". ").append(
                        getQuantityString(
                            R.plurals.number_incorrectly_moved_to_rubbish,
                            errorCount,
                            errorCount
                        )
                    ).toString()
                }
            }
    }

    /**
     * Result of a movement from the Rubbish Bin to the original location.
     */
    class Restoration(
        count: Int,
        errorCount: Int,
        val destination: MegaNode?,
    ) : MoveRequestResult(
        count = count,
        errorCount = errorCount
    ) {
        override fun getResultText(): String =
            when {
                count == 1 && isSuccess -> {

                    if (destination != null) {
                        getString(R.string.context_correctly_node_restored, destination.name)
                    } else {
                        getString(R.string.context_correctly_moved)
                    }
                }
                count == 1 -> {
                    getString(R.string.context_no_restored)
                }
                isSuccess -> {
                    getQuantityString(
                        R.plurals.number_correctly_restored_from_rubbish,
                        count,
                        count
                    )
                }
                count == errorCount -> {
                    getQuantityString(
                        R.plurals.number_incorrectly_restored_from_rubbish,
                        errorCount,
                        errorCount
                    )
                }
                errorCount == 1 -> {
                    getQuantityString(
                        R.plurals.nodes_correctly_and_node_incorrectly_restored_from_rubbish,
                        successCount,
                        successCount
                    )
                }
                successCount == 1 -> {
                    getQuantityString(
                        R.plurals.node_correctly_and_nodes_incorrectly_restored_from_rubbish,
                        errorCount,
                        errorCount
                    )
                }
                else -> {
                    StringBuilder().append(
                        getQuantityString(
                            R.plurals.number_correctly_restored_from_rubbish,
                            successCount,
                            successCount
                        )
                    ).append(". ").append(
                        getQuantityString(
                            R.plurals.number_incorrectly_restored_from_rubbish,
                            errorCount,
                            errorCount
                        )
                    ).toString()
                }
            }
    }
}
