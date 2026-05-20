package mega.privacy.android.app.presentation.node.model

/**
 * Result of a request to move a node to the rubbish bin or remove it permanently,
 * emitted from a ViewModel to its hosting Activity.
 */
sealed interface MoveOrRemoveNodeResult {

    /**
     * The user is offline, so the operation could not be attempted.
     */
    data object Offline : MoveOrRemoveNodeResult

    /**
     * The node lives outside the rubbish bin and the activity should prompt the
     * user to confirm moving it to the rubbish bin.
     */
    data class ConfirmMoveToRubbish(val handle: Long) : MoveOrRemoveNodeResult

    /**
     * The node already lives inside the rubbish bin and the activity should
     * prompt the user to confirm removing it permanently.
     */
    data class ConfirmRemoveFromMega(val handle: Long) : MoveOrRemoveNodeResult

    /**
     * The node was moved to the rubbish bin successfully.
     */
    data object MovedToRubbish : MoveOrRemoveNodeResult

    /**
     * Moving the node to the rubbish bin failed.
     */
    data object MoveFailed : MoveOrRemoveNodeResult

    /**
     * The destination is a foreign node that is over quota.
     */
    data object ForeignNodeOverQuota : MoveOrRemoveNodeResult

    /**
     * The node was permanently removed successfully.
     */
    data object Removed : MoveOrRemoveNodeResult

    /**
     * Permanently removing the node failed.
     */
    data object RemoveFailed : MoveOrRemoveNodeResult
}
