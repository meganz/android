package mega.privacy.android.feature.clouddrive.presentation.folderlink.model

import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.TypedNode

sealed interface FolderLinkContentState {
    data object Loading : FolderLinkContentState

    data class DecryptionKeyRequired(
        val url: String,
        val isKeyIncorrect: Boolean = false,
    ) : FolderLinkContentState

    data object Expired : FolderLinkContentState

    data object Unavailable : FolderLinkContentState

    data class Loaded(
        val items: List<NodeUiItem<TypedNode>> = emptyList(),
    ) : FolderLinkContentState
}
