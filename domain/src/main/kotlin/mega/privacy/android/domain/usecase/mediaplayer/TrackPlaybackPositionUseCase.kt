package mega.privacy.android.domain.usecase.mediaplayer

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.mediaplayer.PlaybackInformation
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.GetTickerUseCase
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * The use case for tracking playback
 */
class TrackPlaybackPositionUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val getTickerUseCase: GetTickerUseCase,
) {

    /**
     * Track the playback information
     *
     * @param getCurrentPlaybackInformation get current playback information
     */
    suspend operator fun invoke(getCurrentPlaybackInformation: () -> PlaybackInformation) {
        getTickerUseCase(TimeUnit.SECONDS.toMillis(1)).map { getCurrentPlaybackInformation() }
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