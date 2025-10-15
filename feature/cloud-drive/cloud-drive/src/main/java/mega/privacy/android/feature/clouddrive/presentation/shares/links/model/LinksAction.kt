package mega.privacy.android.feature.clouddrive.presentation.shares.links.model

import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Links action
 * This interface defines the UI actions that can be performed in the links screen.
 */
sealed interface LinksAction {
    /**
     * Item clicked action
     */
    data class ItemClicked(val nodeUiItem: NodeUiItem<TypedNode>) : LinksAction

    /**
     * Item long clicked action
     */
    data class ItemLongClicked(val nodeUiItem: NodeUiItem<TypedNode>) : LinksAction

    /**
     * Change view type clicked action
     */
    data object ChangeViewTypeClicked : LinksAction

    /**
     * Opened file node handled action
     */
    data object OpenedFileNodeHandled : LinksAction

    /**
     * Select all items action
     */
    data object SelectAllItems : LinksAction

    /**
     * Deselect all items action
     */
    data object DeselectAllItems : LinksAction

    /**
     * Navigate to folder event consumed action
     */
    data object NavigateToFolderEventConsumed : LinksAction

    /**
     * Navigate back event consumed action
     */
    data object NavigateBackEventConsumed : LinksAction
}