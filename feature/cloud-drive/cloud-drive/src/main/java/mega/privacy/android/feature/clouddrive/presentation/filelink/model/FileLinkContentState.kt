package mega.privacy.android.feature.clouddrive.presentation.filelink.model

sealed interface FileLinkContentState {
    data object Loading : FileLinkContentState

    data class DecryptionKeyRequired(
        val url: String,
        val isKeyIncorrect: Boolean = false,
    ) : FileLinkContentState

    data object Expired : FileLinkContentState

    data object Unavailable : FileLinkContentState

    data class Loaded(
        val iconRes: Int,
        val formattedDuration: String? = null,
    ) : FileLinkContentState
}
