package mega.privacy.android.app.mediaplayer.gateway

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.mediaplayer.model.PlaybackPositionState
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode

/**
 * The service gateway for video player
 */
interface VideoPlayerServiceGateway : MediaPlayerServiceGateway {
    /**
     * Close video player UI
     */
    fun mainPlayerUIClosed()

    /**
     * video size update
     *
     * @return Flow<Pair<Int, Int>> first is video width, second is video height
     */
    fun videoSizeUpdate(): Flow<Pair<Int, Int>>

    /**
     * Set repeat mode for video
     *
     * @param repeatToggleMode RepeatToggleMode
     */
    fun setRepeatModeForVideo(repeatToggleMode: RepeatToggleMode)

    /**
     * Update playback position state
     *
     * @return Flow<PlaybackPositionState>
     */
    fun playbackPositionStateUpdate(): Flow<PlaybackPositionState>

    /**
     * Set resume playback position history for playing video
     *
     * @param playbackPosition playback position history
     */
    fun setResumePlaybackPosition(playbackPosition: Long?)

    /**
     * Set resume playback position history for playing video before build sources
     *
     * @param playbackPosition playback position history
     */
    fun setResumePlaybackPositionBeforeBuildSources(playbackPosition: Long?)

    /**
     * Set restart to play video
     */
    fun setRestartPlayVideo()

    /**
     * Set restart to play video before build sources
     */
    fun setRestartPlayVideoBeforeBuildSources()

    /**
     * Cancel playback position dialog
     */
    fun cancelPlaybackPositionDialog()

    /**
     * Cancel playback position dialog before build sources
     */
    fun cancelPlaybackPositionDialogBeforeBuildSources()

    /**
     * Add subtitle for video
     *
     * @param subtitleFileUrl the subtitle file link
     */
    fun addSubtitle(subtitleFileUrl: String)

    /**
     * For show subtitle after the subtitle file has been set
     */
    fun showSubtitle()

    /**
     * For hide subtitle after the subtitle file has been set
     */
    fun hideSubtitle()
}