package mega.privacy.android.feature.clouddrive.presentation.filelink.model

import mega.privacy.android.domain.entity.node.TypedFileNode

sealed interface FileLinkContentState {
    data object Loading : FileLinkContentState

    data class DecryptionKeyRequired(
        val url: String,
        val isKeyIncorrect: Boolean = false,
    ) : FileLinkContentState

    data object Expired : FileLinkContentState

    data object Unavailable : FileLinkContentState

    data class Loaded(
        val fileNode: TypedFileNode,
        val iconRes: Int,
    ) : FileLinkContentState
}
