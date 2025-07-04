package mega.privacy.android.data.mapper

import mega.privacy.android.data.database.entity.MediaPlaybackInfoEntity
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.entity.mediaplayer.MediaType
import javax.inject.Inject

/**
 * Mapper for converting to MediaPlaybackInfo
 */
class MediaPlaybackInfoMapper @Inject constructor() {

    operator fun invoke(entity: MediaPlaybackInfoEntity) = MediaPlaybackInfo(
        mediaHandle = entity.mediaHandle,
        totalDuration = entity.totalDuration,
        currentPosition = entity.currentPosition,
        mediaType = entity.mediaType
    )
}