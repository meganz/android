package mega.privacy.android.feature.clouddrive.presentation.shares.links.model

/**
 * Links action
 * This interface defines the UI actions that can be performed in the links screen.
 */
sealed interface LinksAction {
    /**
     * Change view type clicked action
     */
    data object ChangeViewTypeClicked : LinksAction
}
