package mega.privacy.android.feature.clouddrive.presentation.folderlink.model

import androidx.compose.runtime.Immutable
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.preference.ViewType

@Immutable
data class FolderLinkUiState(
    val contentState: FolderLinkContentState = FolderLinkContentState.Loading,
    val isFolderLoggedIn: Boolean = false,
    val hasCredentials: Boolean = false,
    val currentViewType: ViewType = ViewType.LIST,
    val selectedSortOrder: SortOrder = SortOrder.ORDER_DEFAULT_ASC,
    val selectedSortConfiguration: NodeSortConfiguration = NodeSortConfiguration.default,
    val navigateBackEvent: StateEvent = consumed,
    val rootNode: TypedFolderNode? = null,
    val currentFolderNode: TypedFolderNode? = null,
    val openedFileNode: TypedFileNode? = null,
) {
    val title = LocalizedText.Literal(currentFolderNode?.name ?: "MEGA") // TODO
}
