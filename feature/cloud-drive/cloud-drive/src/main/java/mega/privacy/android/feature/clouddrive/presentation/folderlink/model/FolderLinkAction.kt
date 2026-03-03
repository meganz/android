package mega.privacy.android.feature.clouddrive.presentation.folderlink.model

sealed interface FolderLinkAction {
    data class DecryptionKeyEntered(val key: String) : FolderLinkAction
    data object DecryptionKeyDialogDismissed : FolderLinkAction
}
