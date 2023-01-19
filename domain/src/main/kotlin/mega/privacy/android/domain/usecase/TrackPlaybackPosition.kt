package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.mediaplayer.PlaybackInformation

/**
 * The use case for tracking playback
 */
interface TrackPlaybackPosition {

    /**
     * Track the playback information
     *
     * @param getCurrentPlaybackInformation get current playback information
     */
    suspend operator fun invoke(getCurrentPlaybackInformation: () -> PlaybackInformation)
}