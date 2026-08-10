package mega.privacy.android.feature.clouddrive.presentation.filelink.model

sealed interface FileLinkAction {
    data class DecryptionKeyEntered(val key: String) : FileLinkAction
    data object DecryptionKeyDialogDismissed : FileLinkAction
    data object AutoOpenPreviewConsumed : FileLinkAction
}
