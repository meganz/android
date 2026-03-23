package mega.privacy.android.feature.clouddrive.presentation.folderlink.model

import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeUiItem
import mega.privacy.android.domain.entity.node.TypedNode

sealed interface FolderLinkAction {
    data class DecryptionKeyEntered(val key: String) : FolderLinkAction
    data object DecryptionKeyDialogDismissed : FolderLinkAction
    data class ItemClicked(val nodeUiItem: NodeUiItem<TypedNode>) : FolderLinkAction
    data class ItemLongClicked(val nodeUiItem: NodeUiItem<TypedNode>) : FolderLinkAction
    data object SelectAllItems : FolderLinkAction
    data object DeselectAllItems : FolderLinkAction
    data object BackPressed : FolderLinkAction
    data object NavigateBackEventConsumed : FolderLinkAction
    data object OpenedFileNodeHandled : FolderLinkAction
    data class SortOrderChanged(val sortConfiguration: NodeSortConfiguration) : FolderLinkAction
    data object ChangeViewTypeClicked : FolderLinkAction
}
