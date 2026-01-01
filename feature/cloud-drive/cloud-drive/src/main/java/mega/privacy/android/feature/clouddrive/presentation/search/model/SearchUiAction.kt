package mega.privacy.android.feature.clouddrive.presentation.search.model

import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Search action
 * This interface defines the UI actions that can be performed in the search screen.
 */
sealed interface SearchUiAction {

    /**
     * Update search text action - updates text field immediately without triggering search
     */
    data class UpdateSearchText(val text: String) : SearchUiAction

    /**
     * Item clicked action
     */
    data class ItemClicked(val nodeUiItem: NodeUiItem<TypedNode>) : SearchUiAction

    /**
     * Item long clicked action
     */
    data class ItemLongClicked(val nodeUiItem: NodeUiItem<TypedNode>) : SearchUiAction

    /**
     * Change view type clicked action
     */
    data object ChangeViewTypeClicked : SearchUiAction

    /**
     * Opened file node handled action
     */
    data object OpenedFileNodeHandled : SearchUiAction

    /**
     * Select all items action
     */
    data object SelectAllItems : SearchUiAction

    /**
     * Deselect all items action
     */
    data object DeselectAllItems : SearchUiAction

    /**
     * Navigate to folder event consumed action
     */
    data object NavigateToFolderEventConsumed : SearchUiAction

    /**
     * Navigate back event consumed action
     */
    data object NavigateBackEventConsumed : SearchUiAction

    /**
     * Consume transfer over quota warning action
     */
    data object OverQuotaConsumptionWarning : SearchUiAction
}