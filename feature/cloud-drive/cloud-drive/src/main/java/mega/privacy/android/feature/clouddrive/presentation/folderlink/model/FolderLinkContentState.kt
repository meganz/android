package mega.privacy.android.feature.clouddrive.presentation.folderlink.model

import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode

sealed interface FolderLinkContentState {
    data object Loading : FolderLinkContentState

    data object FolderLogged : FolderLinkContentState

    data class DecryptionKeyRequired(
        val url: String,
        val isKeyIncorrect: Boolean = false,
    ) : FolderLinkContentState

    data object Expired : FolderLinkContentState

    data object Unavailable : FolderLinkContentState

    data class Loaded(
        val title: LocalizedText = LocalizedText.Literal(""),
        val currentFolderId: NodeId = NodeId(-1L),
        val items: List<NodeUiItem<TypedNode>> = emptyList(),
        val rootNode: TypedFolderNode? = null,
        val parentNode: TypedFolderNode? = null,
        val hasDbCredentials: Boolean = false,
    ) : FolderLinkContentState
}
