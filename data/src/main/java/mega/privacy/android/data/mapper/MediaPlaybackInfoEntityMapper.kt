package mega.privacy.android.data.mapper

import mega.privacy.android.data.database.entity.MediaPlaybackInfoEntity
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import javax.inject.Inject

/**
 * Mapper for converting to MediaPlaybackInfoEntity
 */
class MediaPlaybackInfoEntityMapper @Inject constructor() {

    operator fun invoke(info: MediaPlaybackInfo) = MediaPlaybackInfoEntity(
        mediaHandle = info.mediaHandle,
        totalDuration = info.totalDuration,
        currentPosition = info.currentPosition,
        mediaType = info.mediaType
    )
}