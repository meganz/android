package mega.privacy.android.feature.clouddrive.presentation.filelink.model

import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailData

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
        val thumbnailData: ThumbnailData? = null,
        val formattedDuration: String? = null,
        val isVideo: Boolean = false,
    ) : FileLinkContentState
}
