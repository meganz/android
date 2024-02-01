package mega.privacy.android.domain.entity.node

/**
 * Sealed class containing all the info related to a Chat request.
 * The class can be a [ChatRequestAttachNode]
 * @property count Number of requests
 * @property errorCount Number of requests which finished with an error.
 */
sealed class ChatRequestResult(
    val count: Int,
    val errorCount: Int,
) {
    /**
     * Count of success chat request
     */
    val successCount = count - errorCount

    /**
     * Checks whether chat request has only one request
     */
    val isSingleAction: Boolean = count == 1

    /**
     * Checks whether all chat request are all success
     */
    val isSuccess: Boolean = errorCount == 0

    /**
     * Checks whether chat request has data
     */
    val hasNoData: Boolean = count == 0

    /**
     * Checks whether all chat request are errors
     */
    val isAllRequestError: Boolean = errorCount == count

    /**
     * Result of a Attach request to chat
     */
    class ChatRequestAttachNode(
        count: Int,
        errorCount: Int,
    ) : ChatRequestResult(count, errorCount)
}