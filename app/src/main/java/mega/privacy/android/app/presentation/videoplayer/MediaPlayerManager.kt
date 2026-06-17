package mega.privacy.android.app.presentation.videoplayer

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import mega.privacy.android.app.di.mediaplayer.VideoPlayer
import mega.privacy.android.app.mediaplayer.gateway.MediaPlayerGateway
import mega.privacy.android.app.mediaplayer.model.MediaPlaySources
import mega.privacy.android.app.mediaplayer.model.SpeedPlaybackItem
import mega.privacy.android.app.mediaplayer.service.MediaPlayerCallback
import mega.privacy.android.app.presentation.videoplayer.model.MediaPlaybackState
import mega.privacy.android.app.presentation.videoplayer.model.VideoSize
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import javax.inject.Inject

/**
 * Owns the video player's [ExoPlayer] and brokers all access to the underlying [MediaPlayerGateway],
 * keeping the player-construction boilerplate and raw-event mapping out of the ViewModel.
 *
 * Translates the gateway's low-level [MediaPlayerCallback] events into the lightweight callbacks the
 * ViewModel supplies, and delegates the remaining player operations straight to the gateway.
 */
class MediaPlayerManager @Inject constructor(
    @VideoPlayer private val mediaPlayerGateway: MediaPlayerGateway,
) {
    /**
     * Create the [ExoPlayer], forwarding player events to the supplied callbacks.
     *
     * @param onMetadataChanged invoked with the latest track metadata (title / artist / album).
     * @param onMediaItemTransition invoked when the current media item changes.
     * @param onRepeatModeChanged invoked when the repeat mode changes.
     * @param onPlayWhenReadyChanged invoked with the mapped [MediaPlaybackState] and whether the
     * pause was user-initiated.
     * @param onPlaybackStateChanged invoked with the raw ExoPlayer playback state.
     * @param onPlayerError invoked with the player error code.
     * @param onVideoSizeChanged invoked with a non-zero [VideoSize]; zero sizes are ignored.
     */
    fun createPlayer(
        onMetadataChanged: (title: String?, artist: String?, album: String?) -> Unit,
        onMediaItemTransition: (handle: String?, isUpdateName: Boolean) -> Unit,
        onRepeatModeChanged: (repeatToggleMode: RepeatToggleMode) -> Unit,
        onPlayWhenReadyChanged: (state: MediaPlaybackState, isPausedByUser: Boolean) -> Unit,
        onPlaybackStateChanged: (state: Int) -> Unit,
        onPlayerError: (errorCode: Int) -> Unit,
        onVideoSizeChanged: (videoSize: VideoSize) -> Unit,
    ): ExoPlayer = mediaPlayerGateway.createPlayer(
        repeatToggleMode = RepeatToggleMode.REPEAT_NONE,
        nameChangeCallback = onMetadataChanged,
        mediaPlayerCallback = object : MediaPlayerCallback {
            override fun onMediaItemTransitionCallback(handle: String?, isUpdateName: Boolean) =
                onMediaItemTransition(handle, isUpdateName)

            override fun onShuffleModeEnabledChangedCallback(shuffleModeEnabled: Boolean) {}

            override fun onRepeatModeChangedCallback(repeatToggleMode: RepeatToggleMode) =
                onRepeatModeChanged(repeatToggleMode)

            override fun onPlayWhenReadyChangedCallback(playWhenReady: Boolean, reason: Int) {
                val isPausedByUser =
                    reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST && !playWhenReady
                onPlayWhenReadyChanged(
                    if (playWhenReady) MediaPlaybackState.Playing else MediaPlaybackState.Paused,
                    isPausedByUser,
                )
            }

            override fun onPlaybackStateChangedCallback(state: Int) = onPlaybackStateChanged(state)

            override fun onPlayerErrorCallback(errorCode: Int) = onPlayerError(errorCode)

            override fun onVideoSizeCallback(videoWidth: Int, videoHeight: Int) {
                if (videoWidth == 0 || videoHeight == 0) return
                onVideoSizeChanged(VideoSize(videoWidth, videoHeight))
            }
        },
    )

    /** Retry playback after a player error. */
    fun retry() = mediaPlayerGateway.mediaPlayerRetry(true)

    /** Stop and release the player. Call when the owning ViewModel is cleared. */
    fun release() {
        mediaPlayerGateway.playerStop()
        mediaPlayerGateway.playerRelease()
    }

    /** The current media item, or null if none. */
    fun getCurrentMediaItem(): MediaItem? = mediaPlayerGateway.getCurrentMediaItem()

    /** Duration of the current item, in ms. */
    fun getCurrentItemDuration(): Long = mediaPlayerGateway.getCurrentItemDuration()

    /** Current playback position, in ms. */
    fun getCurrentPlayingPosition(): Long = mediaPlayerGateway.getCurrentPlayingPosition()

    /** Set whether playback should proceed when ready. */
    fun setPlayWhenReady(playWhenReady: Boolean) =
        mediaPlayerGateway.setPlayWhenReady(playWhenReady)

    /** Whether playback proceeds when ready. */
    fun getPlayWhenReady(): Boolean = mediaPlayerGateway.getPlayWhenReady()

    /** Seek to the item at [index]. */
    fun playerSeekTo(index: Int) = mediaPlayerGateway.playerSeekTo(index)

    /** Seek to [positionInMs] within the current item. */
    fun playerSeekToPositionInMs(positionInMs: Long) =
        mediaPlayerGateway.playerSeekToPositionInMs(positionInMs)

    /** Build the play sources for the player. */
    fun buildPlaySources(mediaPlaySources: MediaPlaySources) =
        mediaPlayerGateway.buildPlaySources(mediaPlaySources)

    /** Prepare the player. */
    fun playerPrepare() = mediaPlayerGateway.playerPrepare()

    /** Set the repeat mode. */
    fun setRepeatToggleMode(repeatToggleMode: RepeatToggleMode) =
        mediaPlayerGateway.setRepeatToggleMode(repeatToggleMode)

    /** Whether the player is currently playing. */
    fun mediaPlayerIsPlaying(): Boolean = mediaPlayerGateway.mediaPlayerIsPlaying()

    /** Update the playback speed. */
    fun updatePlaybackSpeed(item: SpeedPlaybackItem) =
        mediaPlayerGateway.updatePlaybackSpeed(item)

    /** Add a subtitle file; returns true on success. */
    fun addSubtitle(subtitleFileUrl: String): Boolean =
        mediaPlayerGateway.addSubtitle(subtitleFileUrl)

    /** Show the subtitle after the subtitle file has been set. */
    fun showSubtitle() = mediaPlayerGateway.showSubtitle()

    /** Hide the subtitle after the subtitle file has been set. */
    fun hideSubtitle() = mediaPlayerGateway.hideSubtitle()
}
