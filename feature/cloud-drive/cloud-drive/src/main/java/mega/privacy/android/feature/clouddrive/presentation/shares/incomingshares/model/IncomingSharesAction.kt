package mega.privacy.android.feature.clouddrive.presentation.shares.incomingshares.model

/**
 * Incoming shares action
 * This interface defines the UI actions that can be performed in the incoming shares screen.
 */
sealed interface IncomingSharesAction {
    /**
     * Change view type clicked action
     */
    data object ChangeViewTypeClicked : IncomingSharesAction

    /**
     * Navigate back event consumed action
     */
    data object NavigateBackEventConsumed : IncomingSharesAction
}
