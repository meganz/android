package mega.privacy.android.app.mediaplayer.gateway

import mega.privacy.android.domain.entity.mediaplayer.PlaybackInformation
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo

/**
 * PlayerServiceViewModelGateway for visit VideoPlayerServiceViewModel from outside
 */
interface VideoPlayerServiceViewModelGateway : PlayerServiceViewModelGateway {
    /**
     * Get video repeat Mode
     *
     * @return RepeatToggleMode
     */
    fun videoRepeatToggleMode(): RepeatToggleMode


    /**
     * Set repeat mode for video
     *
     * @param repeatToggleMode RepeatToggleMode
     */
    fun setVideoRepeatMode(repeatToggleMode: RepeatToggleMode)


    /**
     * Track the playback information
     *
     * @param getCurrentPlaybackInformation get current playback information
     */
    suspend fun trackPlayback(getCurrentPlaybackInformation: () -> PlaybackInformation)

    /**
     * Save the playback times
     */
    suspend fun savePlaybackTimes()

    /**
     * Delete playback information
     *
     * @param mediaId the media id of deleted item
     */
    suspend fun deletePlaybackInformation(mediaId: Long)

    /**
     * Monitor playback times
     *
     * @param mediaId the media id of target media item
     * @param seekToPosition the callback for seek to playback position history. If the current item contains the playback history,
     * then invoke the callback and the playback position history is parameter
     */
    suspend fun monitorPlaybackTimes(mediaId: Long?, seekToPosition: (positionInMs: Long?) -> Unit)

    /**
     * Get the subtitle file info that is same name as playing media item
     *
     * @return SubtitleFileInfo
     */
    suspend fun getMatchedSubtitleFileInfoForPlayingItem(): SubtitleFileInfo?

    /**
     * Send VideoPlayerActivatedEvent
     */
    fun sendVideoPlayerActivatedEvent()
}