package mega.privacy.android.app.presentation.movenode

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
    )
}
