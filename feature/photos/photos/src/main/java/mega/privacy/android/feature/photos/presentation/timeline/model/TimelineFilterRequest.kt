package mega.privacy.android.feature.photos.presentation.timeline.model

import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.FilterMediaType

data class TimelineFilterRequest(
    val isRemembered: Boolean,
    val mediaType: FilterMediaType,
    val mediaSource: FilterMediaSource,
)
