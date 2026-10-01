package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.mediaplayer.PlaybackInformation
import mega.privacy.android.domain.repository.MediaPlayerRepository
import javax.inject.Inject

/**
 * The use case for monitor playback times
 */
class MonitorPlaybackTimesUseCase @Inject constructor(
    private val mediaPlayerRepository: MediaPlayerRepository
) {

    /**
     * Monitor the playback times
     *
     * @return Flow<Map<Long, PlaybackInformation>?>
     */
    operator fun invoke(): Flow<Map<Long, PlaybackInformation>?> =
        mediaPlayerRepository.monitorPlaybackTimes()
}