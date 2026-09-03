package mega.privacy.android.domain.usecase.mediaplayer.videoplayer

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.continuewhereleftoff.CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS
import mega.privacy.android.domain.entity.continuewhereleftoff.CWLO_NEAR_COMPLETION_THRESHOLD_MS
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo.Companion.INVALID_MEDIA_HANDLE
import mega.privacy.android.domain.entity.mediaplayer.PlaybackInformation
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.GetTickerUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.RemoveRecentlyUsedItemUseCase
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * The use case for tracking playback
 */
class TrackPlaybackPositionUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val removeRecentlyUsedItemUseCase: RemoveRecentlyUsedItemUseCase,
    private val getTickerUseCase: GetTickerUseCase,
) {

    /**
     * Track the playback information
     *
     * @param getCurrentPlaybackInfo get the current playback snapshot
     */
    suspend operator fun invoke(getCurrentPlaybackInfo: () -> MediaPlaybackInfo) {
        getTickerUseCase(TimeUnit.SECONDS.toMillis(1)).map { getCurrentPlaybackInfo() }
            .filter {
                // A snapshot taken before the player is prepared carries no handle, and nothing
                // is tracked until the position is past the resumable threshold.
                it.mediaHandle != INVALID_MEDIA_HANDLE
                        && it.currentPosition > CWLO_MINIMUM_PLAYBACK_THRESHOLD_MS
            }.collect { info ->
                // When the video is playing until last 3 seconds, remove playback information
                // and drop it from the Continue Where Left Off index so it is not surfaced
                // back to the user as resumable.
                if (info.totalDuration - info.currentPosition < CWLO_NEAR_COMPLETION_THRESHOLD_MS) {
                    mediaPlayerRepository.deletePlaybackInformation(info.mediaHandle)
                    removeRecentlyUsedItemUseCase(info.mediaHandle)
                } else {
                    mediaPlayerRepository.updatePlaybackInformation(
                        PlaybackInformation(
                            mediaId = info.mediaHandle,
                            totalDuration = info.totalDuration,
                            currentPosition = info.currentPosition,
                        )
                    )
                }
            }
    }
}
