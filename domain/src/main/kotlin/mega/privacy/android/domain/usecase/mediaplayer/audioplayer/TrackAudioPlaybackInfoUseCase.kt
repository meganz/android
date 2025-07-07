package mega.privacy.android.domain.usecase.mediaplayer.audioplayer

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.repository.MediaPlayerRepository
import mega.privacy.android.domain.usecase.GetTickerUseCase
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class TrackAudioPlaybackInfoUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository,
    private val getTickerUseCase: GetTickerUseCase,
) {
    /**
     * Track audio playback info
     *
     * @param getCurrentPlaybackInfo get current playback info
     */
    suspend operator fun invoke(getCurrentPlaybackInfo: () -> MediaPlaybackInfo) {
        getTickerUseCase(TimeUnit.SECONDS.toMillis(1)).map { getCurrentPlaybackInfo() }
            .filter {
                // Start update the playback information when the video position is more than 15 seconds
                it.currentPosition > TimeUnit.MINUTES.toMillis(15)
            }.collect {
                // When the video is playing until last 2 seconds, remove playback information
                if (it.totalDuration - it.currentPosition < TimeUnit.SECONDS.toMillis(2)) {
                    mediaPlayerRepository.deleteMediaPlaybackInfo(it.mediaHandle)
                } else {
                    mediaPlayerRepository.updateAudioPlaybackInfo(it)
                }
            }
    }
}