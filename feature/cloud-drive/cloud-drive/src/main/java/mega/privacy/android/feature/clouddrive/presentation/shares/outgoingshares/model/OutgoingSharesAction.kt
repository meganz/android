package mega.privacy.android.feature.clouddrive.presentation.shares.outgoingshares.model

/**
 * Outgoing shares action
 * This interface defines the UI actions that can be performed in the outgoing shares screen.
 */
sealed interface OutgoingSharesAction {
    /**
     * Change view type clicked action
     */
    data object ChangeViewTypeClicked : OutgoingSharesAction

    /**
     * Navigate back event consumed action
     */
    data object NavigateBackEventConsumed : OutgoingSharesAction
}
