package mega.privacy.android.feature.photos.presentation

import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.FilterMediaType

data class MediaFilterUiState(
    val isRemembered: Boolean = false,
    val mediaType: FilterMediaType = FilterMediaType.ALL_MEDIA,
    val mediaSource: FilterMediaSource = FilterMediaSource.AllPhotos,
)
