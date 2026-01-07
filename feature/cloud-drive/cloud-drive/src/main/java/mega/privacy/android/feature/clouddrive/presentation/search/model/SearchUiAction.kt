package mega.privacy.android.feature.clouddrive.presentation.search.model

import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Search action
 * This interface defines the UI actions that can be performed in the search screen.
 */
sealed interface SearchUiAction {

    data class UpdateSearchText(val text: String) : SearchUiAction

    data class SelectFilter(val result: SearchFilterResult) : SearchUiAction

    data class ItemClicked(val nodeUiItem: NodeUiItem<TypedNode>) : SearchUiAction

    data class ItemLongClicked(val nodeUiItem: NodeUiItem<TypedNode>) : SearchUiAction

    data class SetSortOrder(val sortConfiguration: NodeSortConfiguration) : SearchUiAction

    data object ChangeViewTypeClicked : SearchUiAction

    data object OpenedFileNodeHandled : SearchUiAction

    data object SelectAllItems : SearchUiAction

    data object DeselectAllItems : SearchUiAction

    data object NavigateToFolderEventConsumed : SearchUiAction

    data object NavigateBackEventConsumed : SearchUiAction

    data object OverQuotaConsumptionWarning : SearchUiAction
}