package mega.privacy.android.app.presentation.copynode

import mega.privacy.android.domain.entity.node.MoveRequestResult

/**
 * Data class containing all the info related to a copy request.
 *
 * @property count The number of copy request
 * @property errorCount The number of copy request which are errors
 */
data class CopyRequestResult(
    val count: Int,
    val errorCount: Int,
) {
    /**
     * Count of success copy request
     */
    val successCount = count - errorCount

    /**
     * Checks whether copy request has data
     */
    val hasNoData: Boolean = count == 0

    /**
     * Checks whether all copy request are errors
     */
    val isAllRequestError: Boolean = errorCount == count

    /**
     * Checks whether all copy request are successful
     */
    val isAllRequestSuccess: Boolean = errorCount == 0
}


/**
 * A temporary extension function to map a [MoveRequestResult] to a [CopyRequestResult]
 * @return [CopyRequestResult]
 */
fun MoveRequestResult.toCopyRequestResult() = CopyRequestResult(count, errorCount)
