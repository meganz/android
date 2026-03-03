package mega.privacy.android.feature.clouddrive.presentation.folderlink.model

import androidx.compose.runtime.Immutable
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.preference.ViewType

@Immutable
data class FolderLinkUiState(
    val contentState: FolderLinkContentState = FolderLinkContentState.Loading,
    val hasCredentials: Boolean = false,
    val currentViewType: ViewType = ViewType.LIST,
    val selectedSortOrder: SortOrder = SortOrder.ORDER_DEFAULT_ASC,
    val selectedSortConfiguration: NodeSortConfiguration = NodeSortConfiguration.default,
) {
    val title = if (contentState is FolderLinkContentState.Loaded) {
        contentState.title
    } else {
        LocalizedText.Literal("Loading") // TODO
    }
}
