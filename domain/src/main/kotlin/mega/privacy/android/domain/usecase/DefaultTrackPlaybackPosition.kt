package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.mediaplayer.PlaybackInformation
import mega.privacy.android.domain.repository.MediaPlayerRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * The use case implementation for track playback
 */
class DefaultTrackPlaybackPosition @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val getTicker: GetTicker,
) : TrackPlaybackPosition {

    override suspend fun invoke(getCurrentPlaybackInformation: () -> PlaybackInformation) {
        getTicker(TimeUnit.SECONDS.toMillis(1)).map { getCurrentPlaybackInformation() }
            .filter {
                // Start update the playback information when the video position is more than 15 seconds
                it.currentPosition > TimeUnit.SECONDS.toMillis(15)
            }.collect {
                // When the video is playing until last 2 seconds, remove playback information
                if (it.totalDuration - it.currentPosition < TimeUnit.SECONDS.toMillis(2)) {
                    it.mediaId?.let { mediaId ->
                        mediaPlayerRepository.deletePlaybackInformation(mediaId)
                    }
                } else {
                    mediaPlayerRepository.updatePlaybackInformation(it)
                }
            }
    }
}