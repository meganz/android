package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.mediaplayer.PlaybackInformation

/**
 * The use case for monitor playback times
 */
fun interface MonitorPlaybackTimes {

    /**
     * Monitor the playback times
     *
     * @return Flow<Map<Long, PlaybackInformation>?>
     */
    operator fun invoke(): Flow<Map<Long, PlaybackInformation>?>
}