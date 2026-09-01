package mega.privacy.android.data.mapper

import mega.privacy.android.data.database.entity.MediaPlaybackInfoEntity
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import javax.inject.Inject

/**
 * Mapper for converting to MediaPlaybackInfoEntity
 */
class MediaPlaybackInfoEntityMapper @Inject constructor() {

    /**
     * @param info the domain playback info to map
     * @param updatedAt epoch millis to stamp the row with, supplied by the caller so the
     * mapping stays a pure function of its inputs
     */
    operator fun invoke(info: MediaPlaybackInfo, updatedAt: Long) = MediaPlaybackInfoEntity(
        mediaHandle = info.mediaHandle,
        totalDuration = info.totalDuration,
        currentPosition = info.currentPosition,
        mediaType = info.mediaType,
        updatedAt = updatedAt,
    )
}
