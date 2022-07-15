package mega.privacy.android.app.presentation.shares.incoming.model

/**
 * Incoming shares UI state
 *
 * @param incomingParentHandle current incoming shares parent handle
 * @param incomingTreeDepth current incoming tree depth
 */
data class IncomingSharesState(
    val incomingParentHandle: Long = -1L,
    val incomingTreeDepth: Int = 0,
) {

    /**
     * Check if we are at the root of the incoming shares page
     *
     * @return true if at the root of the incoming shares page
     */
    fun isFirstNavigationLevel() = incomingTreeDepth == 0
}