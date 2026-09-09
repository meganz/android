package mega.privacy.android.app.mediaplayer.service

import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode

/**
 * For callback from Player.Listener
 */
interface MediaPlayerCallback {
    /**
     * Callback from onMediaItemTransition
     *
     * @param handle MegaNode handle
     * @param isUpdateName true is update the name, otherwise is false
     */
    fun onMediaItemTransitionCallback(handle: String?, isUpdateName: Boolean)

    /**
     * Callback from onShuffleModeEnabledChanged
     *
     * @param shuffleModeEnabled true is shuffle mode enable, otherwise is false
     */
    fun onShuffleModeEnabledChangedCallback(shuffleModeEnabled: Boolean)

    /**
     * Callback from onRepeatModeChanged
     *
     * @param repeatToggleMode RepeatToggleMode
     */
    fun onRepeatModeChangedCallback(repeatToggleMode: RepeatToggleMode)

    /**
     * Callback from onPlayWhenReadyChanged
     *
     * @param playWhenReady true is play when ready, otherwise is false
     */
    fun onPlayWhenReadyChangedCallback(
        playWhenReady: Boolean,
        reason: Int,
    )

    /**
     * Callback from onPlaybackStateChanged
     *
     * @param state playback state
     */
    fun onPlaybackStateChangedCallback(state: Int)

    /**
     * Callback when the position jumps within the item that is already playing, either because
     * the user seeked or because a repeat-one loop restarted it.
     */
    fun onPlaybackPositionJumpedCallback() = Unit

    /**
     * Callback when playback leaves a media item, reporting where that item was left. Its
     * duration is not reported: the player already reports the duration of the item it moved to.
     *
     * @param handle MegaNode handle of the item being left
     * @param positionMs the position the item was left at
     */
    fun onMediaItemLeftCallback(handle: String?, positionMs: Long) = Unit

    /**
     * Callback from onPlayerError
     *
     * @param errorCode the ExoPlayer error code from [androidx.media3.common.PlaybackException]
     */
    fun onPlayerErrorCallback(errorCode: Int)

    /**
     * Callback from onVideoSizeChanged
     *
     * @param videoWidth video width
     * @param videoHeight video height
     */
    fun onVideoSizeCallback(videoWidth: Int, videoHeight: Int)

    /**
     * Callback when the media contains a video track but no video renderer is active,
     * indicating a silent decoder failure (e.g. hardware decoder exceeds capabilities).
     */
    fun onVideoNotRenderedCallback()

    /**
     * Callback fired once per media item when a WebM/MKV file with oversized Matroska
     * clusters is detected. Such files require ExoPlayer to read an entire cluster
     * (~50 s of content) before playback can resume after a seek, and the delay cannot
     * be reduced on the client side.
     *
     * Called at the moment the user initiates the first seek on the affected file, so
     * the warning reaches the user just before the long loading begins.
     */
    fun onLargeClusterSeekWarning() = Unit
}
