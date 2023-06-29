package mega.privacy.android.domain.entity.node

/**
 * Sealed class containing all the info related to a movement request.
 * The class can be a [GeneralMovement], [RubbishMovement]
 *
 * @property count              Number of requests.
 * @property errorCount         Number of requests which finished with an error.
 * @property oldParentHandle    Handle of the old parent.
 * @property nodes              Nodes to move
 */
sealed class MoveRequestResult(
    val count: Int,
    val errorCount: Int,
    val oldParentHandle: Long? = -1L,
    val nodes: List<Long> = emptyList(),
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
        nodes: List<Long> = emptyList(),
    ) : MoveRequestResult(count, errorCount, oldParentHandle, nodes)

    /**
     * Result of a movement to the Rubbish Bin.
     */
    class RubbishMovement constructor(
        count: Int,
        errorCount: Int,
        oldParentHandle: Long?,
        nodes: List<Long> = emptyList(),
    ) : MoveRequestResult(
        count = count,
        errorCount = errorCount,
        oldParentHandle = oldParentHandle,
        nodes = nodes
    )

    /**
     * Result of a delete permanently
     */
    class DeleteMovement(
        count: Int,
        errorCount: Int,
        nodes: List<Long>,
    ) : MoveRequestResult(
        count = count,
        errorCount = errorCount,
        nodes = nodes,
    )

    /**
     * Result of copy
     */
    class Copy(
        count: Int,
        errorCount: Int,
    ) : MoveRequestResult(
        count = count,
        errorCount = errorCount,
    )
}