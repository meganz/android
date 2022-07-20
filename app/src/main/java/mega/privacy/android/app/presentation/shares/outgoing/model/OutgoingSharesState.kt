package mega.privacy.android.app.presentation.shares.outgoing.model

/**
 * Incoming shares UI state
 *
 * @param outgoingParentHandle current outgoing shares parent handle
 * @param outgoingTreeDepth current outgoing tree depth
 */
data class OutgoingSharesState(
    val outgoingParentHandle: Long = -1L,
    val outgoingTreeDepth: Int = 0,
)