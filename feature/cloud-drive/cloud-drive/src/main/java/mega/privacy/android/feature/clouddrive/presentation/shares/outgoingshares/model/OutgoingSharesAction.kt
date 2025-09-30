package mega.privacy.android.feature.clouddrive.presentation.shares.outgoingshares.model

import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Outgoing shares action
 * This interface defines the UI actions that can be performed in the outgoing shares screen.
 */
sealed interface OutgoingSharesAction {
    /**
     * Item clicked action
     */
    data class ItemClicked(val nodeUiItem: NodeUiItem<TypedNode>) : OutgoingSharesAction

    /**
     * Item long clicked action
     */
    data class ItemLongClicked(val nodeUiItem: NodeUiItem<TypedNode>) : OutgoingSharesAction

    /**
     * Change view type clicked action
     */
    data object ChangeViewTypeClicked : OutgoingSharesAction

    /**
     * Opened file node handled action
     */
    data object OpenedFileNodeHandled : OutgoingSharesAction

    /**
     * Select all items action
     */
    data object SelectAllItems : OutgoingSharesAction

    /**
     * Deselect all items action
     */
    data object DeselectAllItems : OutgoingSharesAction

    /**
     * Navigate to folder event consumed action
     */
    data object NavigateToFolderEventConsumed : OutgoingSharesAction

    /**
     * Navigate back event consumed action
     */
    data object NavigateBackEventConsumed : OutgoingSharesAction
}