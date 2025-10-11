package mega.privacy.android.feature.clouddrive.presentation.shares.incomingshares.model

import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Incoming shares action
 * This interface defines the UI actions that can be performed in the incoming shares screen.
 */
sealed interface IncomingSharesAction {
    /**
     * Item clicked action
     */
    data class ItemClicked(val nodeUiItem: NodeUiItem<TypedNode>) : IncomingSharesAction

    /**
     * Item long clicked action
     */
    data class ItemLongClicked(val nodeUiItem: NodeUiItem<TypedNode>) : IncomingSharesAction

    /**
     * Change view type clicked action
     */
    data object ChangeViewTypeClicked : IncomingSharesAction

    /**
     * Select all items action
     */
    data object SelectAllItems : IncomingSharesAction

    /**
     * Deselect all items action
     */
    data object DeselectAllItems : IncomingSharesAction

    /**
     * Navigate to folder event consumed action
     */
    data object NavigateToFolderEventConsumed : IncomingSharesAction

    /**
     * Navigate back event consumed action
     */
    data object NavigateBackEventConsumed : IncomingSharesAction
}