package mega.privacy.android.app.usecase.data

import mega.privacy.android.app.R
import mega.privacy.android.app.utils.DBUtil
import mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode

/**
 * Sealed class containing all the info related to a movement request.
 * The class can be a [GeneralMovement], [RubbishMovement] or [Restoration]
 *
 * @property count              Number of requests.
 * @property errorCount         Number of requests which finished with an error.
 * @property oldParentHandle    Handle of the old parent.
 * @property isForeignNode      True if should show a foreign storage over quota warning, false otherwise.
 */
sealed class MoveRequestResult(
    val count: Int,
    val errorCount: Int,
    val oldParentHandle: Long? = INVALID_HANDLE,
    val isForeignNode: Boolean
) {

    val successCount = count - errorCount
    val isSingleAction: Boolean = count == 1
    val isSuccess: Boolean = errorCount == 0

    abstract fun getResultText(): String

    /**
     * Resets the account details timestamp if some request finished with success.
     */
    fun resetAccountDetailsIfNeeded() {
        if (successCount > 0) {
            DBUtil.resetAccountDetailsTimeStamp()
        }
    }

    /**
     * Result of a movement request from one location to another one.
     */
    class GeneralMovement(
        count: Int,
        errorCount: Int,
        oldParentHandle: Long? = null,
        isForeignNode: Boolean
    ) : MoveRequestResult(
        count = count,
        errorCount = errorCount,
        oldParentHandle = oldParentHandle,
        isForeignNode = isForeignNode
    ) {

        override fun getResultText(): String =
            when {
                count == 1 && isSuccess -> getString(R.string.context_correctly_moved)
                count == 1 -> getString(R.string.context_no_moved)
                isSuccess -> getString(R.string.number_correctly_moved, count)
                else -> getString(
                    R.string.number_correctly_moved,
                    count - errorCount
                ) + getString(R.string.number_incorrectly_moved, errorCount)
            }
    }

    /**
     * Result of a movement to the Rubbish Bin.
     */
    class RubbishMovement constructor(
        count: Int,
        errorCount: Int,
        oldParentHandle: Long?
    ) : MoveRequestResult(
        count = count,
        errorCount = errorCount,
        oldParentHandle = oldParentHandle,
        isForeignNode = false
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
                errorCount == 1 && successCount == 1 -> {
                    getString(R.string.node_correctly_and_node_incorrectly_moved_to_rubbish)
                }
                errorCount == 1 -> {
                    getString(
                        R.string.nodes_correctly_and_node_incorrectly_moved_to_rubbish,
                        successCount
                    )
                }
                successCount == 1 -> {
                    getString(
                        R.string.node_correctly_and_nodes_incorrectly_moved_to_rubbish,
                        errorCount
                    )
                }
                else -> {
                    getString(
                        R.string.nodes_correctly_and_nodes_incorrectly_moved_to_rubbish,
                        successCount,
                        errorCount
                    )
                }
            }
    }

    /**
     * Result of a movement from the Rubbish Bin to the original location.
     */
    class Restoration(
        count: Int,
        errorCount: Int,
        isForeignNode: Boolean,
        val destination: MegaNode?
    ) : MoveRequestResult(
        count = count,
        errorCount = errorCount,
        isForeignNode = isForeignNode
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
                errorCount == 1 && successCount == 1 -> {
                    getString(R.string.node_correctly_and_node_incorrectly_restored_from_rubbish)
                }
                errorCount == 1 -> {
                    getString(
                        R.string.nodes_correctly_and_node_incorrectly_restored_from_rubbish,
                        successCount
                    )
                }
                successCount == 1 -> {
                    getString(
                        R.string.node_correctly_and_nodes_incorrectly_restored_from_rubbish,
                        errorCount
                    )
                }
                else -> {
                    getString(
                        R.string.nodes_correctly_and_nodes_incorrectly_restored_from_rubbish,
                        successCount,
                        errorCount
                    )
                }
            }
    }
}
