package mega.privacy.android.data.mapper

import mega.privacy.android.data.model.MediaPlaybackInfo
import mega.privacy.android.data.model.MediaType
import javax.inject.Inject

/**
 * Mapper for converting to MediaPlaybackInfo
 */
class MediaPlaybackInfoMapper @Inject constructor() {

    operator fun invoke(
        mediaHandle: Long,
        totalDuration: Long = 0L,
        currentPosition: Long = 0L,
        mediaType: MediaType,
    ) = MediaPlaybackInfo(
        mediaHandle = mediaHandle,
        totalDuration = totalDuration,
        currentPosition = currentPosition,
        mediaType = mediaType
    )
}