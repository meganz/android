package mega.privacy.android.feature.clouddrive.presentation.folderlink.model

sealed interface FolderLinkContentState {
    data object Loading : FolderLinkContentState

    data class DecryptionKeyRequired(
        val url: String,
        val isKeyIncorrect: Boolean = false,
    ) : FolderLinkContentState

    data object Expired : FolderLinkContentState

    data object Unavailable : FolderLinkContentState

    data class Loaded(
        val hasDbCredentials: Boolean = false,
    ) : FolderLinkContentState
}
