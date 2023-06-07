package mega.privacy.android.app.presentation.movenode

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.movenode.MoveRequestResult.GeneralMovement
import mega.privacy.android.app.presentation.movenode.MoveRequestResult.RubbishMovement
import nz.mega.sdk.MegaApiJava

/**
 * Sealed class containing all the info related to a movement request.
 * The class can be a [GeneralMovement], [RubbishMovement]
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
        private val context: Context,
    ) : MoveRequestResult(
        count = count,
        errorCount = errorCount,
        oldParentHandle = oldParentHandle
    ) {
        override fun getResultText(): String =
            when {
                count == 1 && isSuccess -> {
                    context.getString(R.string.context_correctly_moved_to_rubbish)
                }
                count == 1 -> {
                    context.getString(R.string.context_no_moved)
                }
                isSuccess -> {
                    context.resources.getQuantityString(
                        R.plurals.number_correctly_moved_to_rubbish,
                        count,
                        count
                    )
                }
                count == errorCount -> {
                    context.resources.getQuantityString(
                        R.plurals.number_incorrectly_moved_to_rubbish,
                        errorCount,
                        errorCount
                    )
                }
                errorCount == 1 -> {
                    context.resources.getQuantityString(
                        R.plurals.nodes_correctly_and_node_incorrectly_moved_to_rubbish,
                        successCount,
                        successCount
                    )
                }
                successCount == 1 -> {
                    context.resources.getQuantityString(
                        R.plurals.node_correctly_and_nodes_incorrectly_moved_to_rubbish,
                        errorCount,
                        errorCount
                    )
                }
                else -> {
                    StringBuilder().append(
                        context.resources.getQuantityString(
                            R.plurals.number_correctly_moved_to_rubbish,
                            successCount,
                            successCount
                        )
                    ).append(". ").append(
                        context.resources.getQuantityString(
                            R.plurals.number_incorrectly_moved_to_rubbish,
                            errorCount,
                            errorCount
                        )
                    ).toString()
                }
            }
    }
}
